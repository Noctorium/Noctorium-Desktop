package app.noctorium.auth

import app.noctorium.vk.VkCookies

/*
 * What signing in to VK in the embedded browser is watching for.
 *
 * VK's session is two cookies on two hosts: `remixsid` on vk.ru, and `p` on login.vk.ru, which VK's sign-in
 * sets on its own host and which is easy to miss for that reason -- without it VK answers as though nobody
 * had signed in. So the cookie store is read for both addresses, and the sign-in counts as done only once
 * both are there.
 *
 * The values are handed to the core and nowhere else: never written to a log, never shown.
 */

/** Where VK's sign-in starts. Its VK ID and two-step pages follow from here and come back on their own. */
const val VK_SIGN_IN = "https://vk.ru/login"

/** Where `remixsid` is read from. */
const val VK_SITE = "https://vk.ru/"

/** Where `p` is read from: VK's sign-in host, not the site's. */
const val VK_LOGIN = "https://login.vk.ru/"

/**
 * The addresses whose cookies make up a VK session, for clearing before a fresh sign-in and after Noctorium
 * has taken the session over -- one holder of a session, as with Google's.
 */
val VK_SESSION_URLS = listOf(
    VK_SITE,
    VK_LOGIN,
    "https://id.vk.ru/",
    "https://m.vk.ru/",
)

/**
 * The session as the core takes it -- `p=…; remixsid=…` -- once the browser holds both live cookies, or null
 * while it does not.
 *
 * [site] is what the store has for [VK_SITE] and [login] what it has for [VK_LOGIN]. Each cookie is taken
 * from its own host's list; a cookie set on all of vk.ru shows up in both, which is fine.
 */
fun vkSessionCookies(site: List<HarvestedCookie>, login: List<HarvestedCookie>): String? {
    val remixsid = site.lastOrNull { it.name == "remixsid" && it.isLive() }?.value ?: return null
    val p = login.lastOrNull { it.name == "p" && it.isLive() }?.value ?: return null
    return VkCookies(p, remixsid).header
}
