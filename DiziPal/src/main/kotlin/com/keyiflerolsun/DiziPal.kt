// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.Qualities

class DiziPal : MainAPI() {
    override var mainUrl            = "https://dizipal671.com"
    override var name               = "DiziPal"
    override val hasMainPage        = true
    override var lang               = "tr"
    override val hasQuickSearch     = false
    override val hasDownloadSupport = true
    override val supportedTypes     = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/diziler/son-bolumler" to "Son Bölümler",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data).document
        val home = document.select("div.episode-item").mapNotNull { it.toSearchResult() }
        
        return newHomePageResponse(request.name, home, hasNext=false)
    }
    
    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("div.name")?.text()?.trim() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/diziler?kelime=${query}&durum=&tur=&type=&siralama=").document
        return document.select("article.type2 ul li").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        //val document = app.get(url).document

        return newMovieLoadResponse("Movie Başlık", "https://dizipal671.com/dizi/deneme-cekimi/sezon-1/bolum-6", TvType.Movie, "https://dizipal671.com/dizi/deneme-cekimi/sezon-1/bolum-6") {
            this.posterUrl = "https://www.themoviedb.org/t/p/original/in9idEuDCHh2FXieGbwlidolB3n.jpg"
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("DZP", "data » ${data}")
        val document = app.get(data).document
        val iframe   = document.selectFirst(".series-player-container iframe")?.attr("src") ?: return false
        Log.d("DZP", "iframe » ${iframe}")

        val i_source = app.get("${iframe}", referer="${mainUrl}/").text
        val m3u_link = Regex("""file:\"([^\"]+)""").find(i_source)?.groupValues?.get(1)
        if (m3u_link == null) {
            Log.d("DZP", "i_source » ${i_source}")
            return false
        }

        val subtitles = Regex("""\"subtitle":\"([^\"]+)""").find(i_source)?.groupValues?.get(1)
        if (subtitles != null) {
            subtitles.split(",").forEach {
                val sub_lang = it.substringAfter("[").substringBefore("]")
                val sub_url  = it.replace("[${sub_lang}]", "")
                subtitleCallback.invoke(
                    SubtitleFile(
                        lang = sub_lang,
                        url  = fixUrl(sub_url)
                    )
                )
            }
        }

        callback.invoke(
            ExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = m3u_link,
                referer = "${mainUrl}/",
                quality = Qualities.Unknown.value,
                isM3u8  = true
            )
        )

        // M3u8Helper.generateM3u8(
        //     source    = this.name,
        //     name      = this.name,
        //     streamUrl = m3u_link,
        //     referer   = "${mainUrl}/"
        // ).forEach(callback)

        return true
    }
}
