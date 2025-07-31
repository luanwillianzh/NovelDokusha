package my.noveldokusha.scraper.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.PagedList
import my.noveldokusha.core.Response
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.scraper.R
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.scraper.domain.ChapterResult
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

class FlaskNovelReader(
    private val networkClient: NetworkClient
) : SourceInterface.Catalog {

    override val id = "flask_novel_reader"
    override val nameStrId = R.string.source_name_light_novel_world // Pode criar outro ID específico
    override val baseUrl = "https://novel-reader-flask.vercel.app"
    override val catalogUrl = "$baseUrl/api/lancamentos"
    override val iconUrl = "" // Pode definir se quiser
    override val language = LanguageCode.PORTUGUESE

    override suspend fun getChapterText(doc: org.jsoup.nodes.Document): String {
        return "" // Não usado com API direta
    }

    override suspend fun getBookCoverImageUrl(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            try {
                val novelId = bookUrl.removePrefix("$baseUrl/api/novel/")
                val json = networkClient.get("$baseUrl/api/novel/$novelId").body?.string()
                    ?: return@withContext Response.Failure("Resposta vazia")

                val obj = JSONObject(json)
                Response.Success("$baseUrl/static/${obj.getString("cover")}")
            } catch (e: Exception) {
                Response.Failure(e.message ?: "Erro desconhecido")
            }
        }

    override suspend fun getBookDescription(bookUrl: String): Response<String?> =
        withContext(Dispatchers.Default) {
            try {
                val novelId = bookUrl.removePrefix("$baseUrl/api/novel/")
                val json = networkClient.get("$baseUrl/api/novel/$novelId").body?.string()
                    ?: return@withContext Response.Failure("Resposta vazia")

                val obj = JSONObject(json)
                Response.Success(obj.getString("desc"))
            } catch (e: Exception) {
                Response.Failure(e.message ?: "Erro ao obter descrição")
            }
        }

    override suspend fun getChapterList(bookUrl: String): Response<List<ChapterResult>> =
        withContext(Dispatchers.Default) {
            try {
                val novelId = bookUrl.removePrefix("$baseUrl/api/novel/")
                val json = networkClient.get("$baseUrl/api/novel/$novelId").body?.string()
                    ?: return@withContext Response.Failure("Resposta vazia")

                val obj = JSONObject(json)
                val chapters = obj.getJSONArray("chapters")

                val result = mutableListOf<ChapterResult>()
                for (i in 0 until chapters.length()) {
                    val (title, url) = chapters.getJSONArray(i).let {
                        it.getString(0) to it.getString(1)
                    }
                    result.add(
                        ChapterResult(
                            title = title,
                            url = "$baseUrl/api/novel/$novelId/chapter/$url"
                        )
                    )
                }

                Response.Success(result)
            } catch (e: Exception) {
                Response.Failure(e.message ?: "Erro ao obter capítulos")
            }
        }

    override suspend fun getCatalogList(index: Int): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            try {
                // Por enquanto, API só fornece lançamentos sem paginação
                if (index > 0) return@withContext Response.Success(PagedList.createEmpty(index))

                val json = networkClient.get(catalogUrl).body?.string()
                    ?: return@withContext Response.Failure("Resposta vazia")

                val arr = JSONObject(json).getJSONArray("resultado")
                val result = mutableListOf<BookResult>()

                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    result.add(
                        BookResult(
                            title = item.getString("nome"),
                            url = "$baseUrl/api/novel/${item.getString("url")}",
                            coverImageUrl = "$baseUrl/static/${item.getString("cover")}"
                        )
                    )
                }

                Response.Success(PagedList(result, index, isLastPage = true))
            } catch (e: Exception) {
                Response.Failure(e.message ?: "Erro ao obter catálogo")
            }
        }

    override suspend fun getCatalogSearch(index: Int, input: String): Response<PagedList<BookResult>> =
        withContext(Dispatchers.Default) {
            try {
                if (index > 0) return@withContext Response.Success(PagedList.createEmpty(index))

                val json = networkClient.get("$baseUrl/api/search/${input}").body?.string()
                    ?: return@withContext Response.Failure("Resposta vazia")

                val arr = JSONObject(json).getJSONArray("resultado")
                val result = mutableListOf<BookResult>()

                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    result.add(
                        BookResult(
                            title = item.getString("nome"),
                            url = "$baseUrl/api/novel/${item.getString("url")}",
                            coverImageUrl = "$baseUrl/static/${item.getString("cover")}"
                        )
                    )
                }

                Response.Success(PagedList(result, index, isLastPage = true))
            } catch (e: Exception) {
                Response.Failure(e.message ?: "Erro ao buscar")
            }
        }
}
