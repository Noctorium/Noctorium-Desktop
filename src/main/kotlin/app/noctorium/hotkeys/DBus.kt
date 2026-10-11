package app.noctorium.hotkeys

import java.io.ByteArrayOutputStream
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/*
 * Just enough D-Bus to talk to the desktop portal: a connection to the session bus over its Unix socket, the
 * wire format for the handful of types the GlobalShortcuts portal uses, method calls and their replies, and the
 * signals the portal answers with.
 *
 * Written here rather than taken from a library because the portal needs one connection kept open for as long
 * as the shortcuts are -- its session belongs to the connection that made it -- which rules out calling a
 * command-line tool, and the Java D-Bus libraries bring native socket code with them for what Java has done by
 * itself since 16.
 */

/** A D-Bus variant: a value with its type written beside it. */
data class Variant(val signature: String, val value: Any?)

/** A message as it arrives: its kind, its header fields and its body, read by its signature. */
internal data class DBusMessage(
    val type: Int,
    val serial: Int,
    val replySerial: Int?,
    val path: String?,
    val iface: String?,
    val member: String?,
    val errorName: String?,
    val signature: String,
    val body: List<Any?>,
) {
    companion object {
        const val METHOD_CALL = 1
        const val METHOD_RETURN = 2
        const val ERROR = 3
        const val SIGNAL = 4
    }
}

/** Writes values in D-Bus's little-endian wire format, aligning each as the format asks. */
internal class DBusWriter(private val start: Int = 0) {
    private val out = ByteArrayOutputStream()
    val size: Int get() = out.size()
    fun bytes(): ByteArray = out.toByteArray()

    private fun align(to: Int) {
        while ((start + out.size()) % to != 0) out.write(0)
    }

    private fun u32(value: Int) {
        align(4)
        out.write(value and 0xFF); out.write((value ushr 8) and 0xFF); out.write((value ushr 16) and 0xFF); out.write((value ushr 24) and 0xFF)
    }

    private fun u64(value: Long) {
        align(8)
        for (i in 0 until 8) out.write(((value ushr (8 * i)) and 0xFF).toInt())
    }

    /** Writes the values of a whole [signature], one complete type after another. */
    fun write(signature: String, values: List<Any?>) {
        val types = DBusSignature.split(signature)
        require(types.size == values.size) { "$signature takes ${types.size} values, not ${values.size}" }
        types.zip(values).forEach { (type, value) -> writeOne(type, value) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun writeOne(type: String, value: Any?) {
        when (type[0]) {
            'y' -> out.write((value as Number).toInt() and 0xFF)
            'b' -> u32(if (value as Boolean) 1 else 0)
            'i', 'u' -> u32((value as Number).toInt())
            'x', 't' -> u64((value as Number).toLong())
            's', 'o' -> {
                val bytes = (value as String).toByteArray(Charsets.UTF_8)
                u32(bytes.size); out.write(bytes); out.write(0)
            }
            'g' -> {
                val bytes = (value as String).toByteArray(Charsets.US_ASCII)
                out.write(bytes.size); out.write(bytes); out.write(0)
            }
            'v' -> {
                val variant = value as Variant
                writeOne("g", variant.signature)
                writeOne(variant.signature, variant.value)
            }
            'a' -> {
                val element = type.substring(1)
                align(4)
                val lengthAt = out.size()
                u32(0)
                align(DBusSignature.alignment(element[0]))
                val first = out.size()
                val items: Collection<Any?> = if (element[0] == '{') (value as Map<Any?, Any?>).entries.map { listOf(it.key, it.value) } else value as List<Any?>
                items.forEach { writeOne(element, it) }
                val length = out.size() - first
                patch(lengthAt, length)
            }
            '(', '{' -> {
                align(8)
                val inner = DBusSignature.split(type.substring(1, type.length - 1))
                val fields = value as List<Any?>
                inner.zip(fields).forEach { (t, v) -> writeOne(t, v) }
            }
            else -> error("Cannot write D-Bus type $type")
        }
    }

    private fun patch(at: Int, value: Int) {
        val bytes = out.toByteArray()
        bytes[at] = (value and 0xFF).toByte(); bytes[at + 1] = ((value ushr 8) and 0xFF).toByte()
        bytes[at + 2] = ((value ushr 16) and 0xFF).toByte(); bytes[at + 3] = ((value ushr 24) and 0xFF).toByte()
        out.reset(); out.write(bytes)
    }
}

/** Reads values from D-Bus's wire format, in either byte order, as Kotlin values: dictionaries as maps. */
internal class DBusReader(private val buffer: ByteBuffer, private val start: Int = 0) {
    private fun align(to: Int) {
        while ((buffer.position() - start) % to != 0) buffer.get()
    }

    fun read(signature: String): List<Any?> = DBusSignature.split(signature).map(::readOne)

    private fun readOne(type: String): Any? = when (type[0]) {
        'y' -> buffer.get().toInt() and 0xFF
        'b' -> { align(4); buffer.int != 0 }
        'n' -> { align(2); buffer.short.toInt() }
        'q' -> { align(2); buffer.short.toInt() and 0xFFFF }
        'i' -> { align(4); buffer.int }
        'u' -> { align(4); buffer.int.toLong() and 0xFFFFFFFFL }
        'x', 't' -> { align(8); buffer.long }
        'd' -> { align(8); buffer.double }
        'h' -> { align(4); buffer.int }
        's', 'o' -> {
            align(4)
            val length = buffer.int
            val bytes = ByteArray(length).also { buffer.get(it) }
            buffer.get()
            String(bytes, Charsets.UTF_8)
        }
        'g' -> {
            val length = buffer.get().toInt() and 0xFF
            val bytes = ByteArray(length).also { buffer.get(it) }
            buffer.get()
            String(bytes, Charsets.US_ASCII)
        }
        'v' -> {
            val signature = readOne("g") as String
            Variant(signature, readOne(signature))
        }
        'a' -> {
            align(4)
            val length = buffer.int
            val element = type.substring(1)
            align(DBusSignature.alignment(element[0]))
            val end = buffer.position() + length
            if (element[0] == '{') {
                buildMap { while (buffer.position() < end) { val (k, v) = readOne(element) as List<*>; put(k, v) } }
            } else {
                buildList { while (buffer.position() < end) add(readOne(element)) }
            }
        }
        '(', '{' -> {
            align(8)
            DBusSignature.split(type.substring(1, type.length - 1)).map(::readOne)
        }
        else -> error("Cannot read D-Bus type $type")
    }
}

/** Splitting signatures into their complete types, and how each type aligns. */
internal object DBusSignature {
    fun split(signature: String): List<String> {
        val types = mutableListOf<String>()
        var i = 0
        while (i < signature.length) {
            val end = endOf(signature, i)
            types += signature.substring(i, end)
            i = end
        }
        return types
    }

    private fun endOf(signature: String, from: Int): Int = when (signature[from]) {
        'a' -> endOf(signature, from + 1)
        '(', '{' -> {
            var depth = 0
            var i = from
            while (true) {
                when (signature[i]) {
                    '(', '{' -> depth++
                    ')', '}' -> { depth--; if (depth == 0) break }
                }
                i++
            }
            i + 1
        }
        else -> from + 1
    }

    fun alignment(code: Char): Int = when (code) {
        'y', 'g', 'v' -> 1
        'n', 'q' -> 2
        'b', 'i', 'u', 's', 'o', 'a', 'h' -> 4
        'x', 't', 'd', '(', '{' -> 8
        else -> 1
    }
}

/**
 * A connection to the session bus: authenticated as this user, said hello to, and read on a thread of its own.
 * Method calls wait for their reply; signals go to [onSignal].
 */
internal class DBusConnection private constructor(private val channel: SocketChannel) : AutoCloseable {
    private val serials = AtomicInteger(1)
    private val replies = ConcurrentHashMap<Int, CompletableFuture<DBusMessage>>()
    @Volatile var onSignal: (DBusMessage) -> Unit = {}
    @Volatile private var open = true
    private val input = Channels.newInputStream(channel)
    private val output = Channels.newOutputStream(channel)
    lateinit var uniqueName: String
        private set

    private val reader = Thread({ readLoop() }, "noctorium-dbus").apply { isDaemon = true }

    private fun start() {
        reader.start()
        uniqueName = call("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello").body.first() as String
    }

    /** Calls [member] and waits for what it returns; an error reply is thrown. */
    fun call(destination: String, path: String, iface: String, member: String, signature: String = "", args: List<Any?> = emptyList()): DBusMessage {
        val serial = serials.getAndIncrement()
        val answer = CompletableFuture<DBusMessage>()
        replies[serial] = answer
        send(DBusMessage.METHOD_CALL, serial, destination, path, iface, member, signature, args)
        val reply = try {
            answer.get(10, TimeUnit.SECONDS)
        } finally {
            replies.remove(serial)
        }
        if (reply.type == DBusMessage.ERROR) error("${reply.errorName}: ${reply.body.firstOrNull() ?: ""}")
        return reply
    }

    @Synchronized
    private fun send(type: Int, serial: Int, destination: String, path: String, iface: String, member: String, signature: String, args: List<Any?>) {
        val body = DBusWriter().apply { if (signature.isNotEmpty()) write(signature, args) }.bytes()
        val fields = buildList {
            add(listOf(1, Variant("o", path)))
            add(listOf(2, Variant("s", iface)))
            add(listOf(3, Variant("s", member)))
            add(listOf(6, Variant("s", destination)))
            if (signature.isNotEmpty()) add(listOf(8, Variant("g", signature)))
        }
        val header = DBusWriter().apply {
            write("yyyyuua(yv)", listOf('l'.code, type, 0, 1, body.size, serial, fields))
        }
        val padded = header.bytes().let { bytes -> bytes + ByteArray((8 - bytes.size % 8) % 8) }
        output.write(padded + body)
        output.flush()
    }

    private fun readLoop() {
        try {
            while (open) {
                val fixed = ByteArray(16).also { readFully(it) }
                val order = if (fixed[0] == 'B'.code.toByte()) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
                val head = ByteBuffer.wrap(fixed).order(order)
                val bodyLength = head.getInt(4)
                val fieldsLength = head.getInt(12)
                val rest = ByteArray(fieldsLength + (8 - (16 + fieldsLength) % 8) % 8 + bodyLength).also { readFully(it) }
                val whole = ByteBuffer.wrap(fixed + rest).order(order)
                whole.position(12)
                val reader = DBusReader(whole)
                @Suppress("UNCHECKED_CAST")
                val fields = (reader.read("a(yv)").first() as List<List<Any?>>).associate { (code, value) -> (code as Int) to (value as Variant).value }
                whole.position(whole.limit() - bodyLength)
                val signature = fields[8] as String? ?: ""
                val message = DBusMessage(
                    type = fixed[1].toInt(),
                    serial = head.getInt(8),
                    replySerial = (fields[5] as Long?)?.toInt(),
                    path = fields[1] as String?,
                    iface = fields[2] as String?,
                    member = fields[3] as String?,
                    errorName = fields[4] as String?,
                    signature = signature,
                    body = if (signature.isEmpty()) emptyList() else DBusReader(whole, whole.position()).read(signature),
                )
                when (message.type) {
                    DBusMessage.METHOD_RETURN, DBusMessage.ERROR -> message.replySerial?.let { replies[it]?.complete(message) }
                    DBusMessage.SIGNAL -> runCatching { onSignal(message) }
                }
            }
        } catch (_: Exception) {
            // The bus went away, or the connection was closed; anybody waiting is told so.
            replies.values.forEach { it.completeExceptionally(IllegalStateException("The session bus closed")) }
        }
    }

    private fun readFully(into: ByteArray) {
        var read = 0
        while (read < into.size) {
            val n = input.read(into, read, into.size - read)
            if (n < 0) error("The session bus closed")
            read += n
        }
    }

    override fun close() {
        open = false
        runCatching { channel.close() }
    }

    companion object {
        /** The session bus's socket, from the environment as the bus advertises it. Null where it is not one Java can open. */
        fun sessionSocket(environment: Map<String, String> = System.getenv()): Path? {
            val address = environment["DBUS_SESSION_BUS_ADDRESS"]
            if (address == null) {
                val runtime = environment["XDG_RUNTIME_DIR"] ?: return null
                return Path.of(runtime, "bus").takeIf(Files::exists)
            }
            // Several addresses may be offered, separated by semicolons; a path is the kind a Java socket can open.
            return address.split(';').firstNotNullOfOrNull { candidate ->
                if (!candidate.startsWith("unix:")) return@firstNotNullOfOrNull null
                candidate.removePrefix("unix:").split(',').associate { it.substringBefore('=') to it.substringAfter('=', "") }["path"]
                    ?.let { Path.of(it) }
            }
        }

        /** Opens the session bus, or fails saying why. */
        fun session(): DBusConnection {
            val socket = sessionSocket() ?: error("No session bus this program can reach")
            val channel = SocketChannel.open(StandardProtocolFamily.UNIX)
            channel.connect(UnixDomainSocketAddress.of(socket))
            authenticate(channel)
            return DBusConnection(channel).also { it.start() }
        }

        /** EXTERNAL authentication: the bus checks the socket's own credentials against the user id given. */
        private fun authenticate(channel: SocketChannel) {
            val uid = Files.getAttribute(Path.of("/proc/self"), "unix:uid").toString()
            val hex = uid.toByteArray().joinToString("") { "%02x".format(it) }
            val output = Channels.newOutputStream(channel)
            output.write(byteArrayOf(0) + "AUTH EXTERNAL $hex\r\n".toByteArray())
            output.flush()
            val line = readLine(channel)
            check(line.startsWith("OK")) { "The session bus refused: $line" }
            output.write("BEGIN\r\n".toByteArray())
            output.flush()
        }

        private fun readLine(channel: SocketChannel): String {
            val bytes = ByteArrayOutputStream()
            val one = ByteBuffer.allocate(1)
            while (true) {
                one.clear()
                if (channel.read(one) < 0) break
                val b = one.get(0).toInt()
                if (b == '\n'.code) break
                if (b != '\r'.code) bytes.write(b)
            }
            return bytes.toString(Charsets.US_ASCII)
        }
    }
}
