package example

import java.io.{File, PrintWriter}
import scala.language.postfixOps

import scala.jdk.CollectionConverters._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.math.BigDecimal

object Main extends Scrapper with App {
  val pw = new PrintWriter(new File("output.csv" ))
  for(page <- 1 to totalPages) {
    val doc = getDoc(urlBase + urlDisciplinas + s"?page=$page")
    val disciplinasDiv = doc.select("div.q-discipline-item")
    val itens = disciplinasDiv.asScala.toSeq.flatMap(disciplinaDiv => {
      val disciplinaElement = disciplinaDiv.select("h3 a")
      val disciplinaTitulo = disciplinaElement.text()
      val disciplinaLink = disciplinaElement.attr("href")
      scrapDisciplina(disciplinaTitulo, disciplinaLink)
    })
    val text = itens.map(_.toString).mkString("\n")
    pw.write(text)
  }
  pw.close
}

trait Scrapper {
  val totalPages = 12
  val urlBase = "https://www.qconcursos.com"
  val urlDisciplinas = "/questoes-de-concursos/disciplinas"

  def getDoc(url: String): Document = {
    Thread.sleep(3000) // Evitar Erro 429 -- Too Many Requests
    val connection = Jsoup.connect(url)
      .header("authority", "www.qconcursos.com")
      .header("method", "GET")
      .header("path", "/questoes-de-concursos/disciplinas?page=1")
      .header("scheme", "https")
      .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
//      .header("accept-encoding", "gzip, deflate, br")
      .header("accept-language", "pt-PT,pt;q=0.9,en-US;q=0.8,en;q=0.7")
      .header("cache-control", "no-cache")
      .header("cookie", "__cfduid=d51a507a168d1bb1c583d58d4a1205a7d1593875392; __cfruid=b1f2a6e2f3236c57be0824136800740f14362383-1593875392; _gcl_au=1.1.143454064.1593875390; _ga=GA1.2.138137813.1593875390; _gid=GA1.2.1306399648.1593875390; _hjid=7585e8ca-6551-499b-9e3b-bdbae348db7c; __ca__chat=UKt9gKwexHc7; _uetsid=0c2404b0-314d-b728-b7d7-b2a20b90bd95; _uetvid=621ff633-b444-bfd1-f788-d68f144e789b; _my_app_session=aE41bUt5TDNwYytld0FicWNWaVB4eXNrdktrTVJ6SjhOdmQwQWF3RGZ0aC9vWjFjQkZMRjc0TmVCb1VqRkN1aWhMRjNsaXJjRFNBN1RBTmc3MWs5bnhrYnBVZldJK21HeGd2TXQ2TnlsZXg3WERJd1FQVWxJaTIvNExURmkxc2YralpjOVNnSGdUS2IvSkVEYUFQdHNRPT0tLTI4K2ozT0FqNHZSMGdxeHBLbUVLc3c9PQ%3D%3D--7256e8bcad5926b44856120d228a27b6e5a738a1")
      .header("pragma", "no-cache")
      .header("sec-fetch-dest", "document")
      .header("sec-fetch-mode", "navigate")
      .header("sec-fetch-site", "none")
      .header("sec-fetch-user", "?1")
      .header("upgrade-insecure-requests", "1")
      .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36")

    val response = connection.execute()
    Jsoup.parse(new String(response.bodyAsBytes(),"UTF-8"))
  }

  def scrapDisciplina(disciplina: String, urlDisciplina: String)= {
    val doc = getDoc(urlBase + urlDisciplina)
    val capitulos = doc.select("div.q-subject-group-item.panel.panel-default")
    capitulos.asScala.toSeq.flatMap(capituloElement => {
      val capituloTitulo = capituloElement.select("div.panel-heading h2.q-title a").text()
      val assuntos = capituloElement.select("ul li")
      if (assuntos.isEmpty) {
        val itens = capituloElement.select("div.q-items div.q-item a")
        val numeroQuestoes = itens.first().text()
        val numeroComentadas = itens.last().text()
        Seq(Item(disciplina, capituloTitulo, capituloTitulo, numeroQuestoes, numeroComentadas))
      } else {
        assuntos.asScala.toSeq.map(assuntoElement => {
          val assuntoTitulo = assuntoElement.select("h3").text()
          val itens = assuntoElement.select("div.q-value a")
          val numeroQuestoes = itens.first().text()
          val numeroComentadas = itens.last().text()
          Item(disciplina, capituloTitulo, assuntoTitulo, numeroQuestoes, numeroComentadas)
        })
      }
    })
  }
}

case class Item(disciplina: String, capitulo: String, assunto: String, numeroQuestoes: String, numeroComentadas: String) {

  override def toString: String = {
    val pattern = "^([0-9]+.?[0-9]*) (.+)"r
    val pattern(numeroCapituloTxt, textoCapitulo) = capitulo
    val numeroCapituloFormatado = numeroFormatado(numeroCapituloTxt)
    val pattern(numeroAssuntoTxt, textoAssunto) = assunto
    val numeroAssuntoFormatado = numeroFormatado(numeroAssuntoTxt)
    Seq(disciplina, s"$numeroCapituloFormatado $textoCapitulo", s"$numeroAssuntoFormatado $textoAssunto", numeroQuestoes, numeroComentadas)
      .map(str => "\"" + str + "\"")
      .mkString(",")
  }

  def numeroFormatado(numero: String) = {
    val numeroSemPontoNoFinal = numero.replaceAll("\\.+$", "") // remove eventual ponto (.) no final do número (ex: 1.)
    BigDecimal(numeroSemPontoNoFinal).formatted("%05.2f")
  }

}
