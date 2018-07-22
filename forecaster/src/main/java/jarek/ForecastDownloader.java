package jarek;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ForecastDownloader {
  public static void main(String[] args) throws Exception {
    new ForecastDownloader().go(args);
  }

  private void go(String[] args) throws Exception {
    String forecast = getInfoFromInternet(args[0]);
    System.out.println("prognoza: " + forecast);
    SimpleDateFormat df = new SimpleDateFormat("YYYYMMdd");
    File outDir = new File("weather_data");
    outDir.mkdir();
    File outFile = new File(outDir, df.format(Calendar.getInstance().getTime()) + ".txt");
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, true), "UTF-8"));
    bw.write(forecast + "\n");
    bw.write("\n");
    bw.close();
  }

  private String getInfoFromInternet(String urlString) throws Exception {
    URL url = new URL(urlString);
    Document doc = url.getProtocol().equals("file") ?
      Jsoup.parse(new File(url.toURI()), "utf-8") :
      Jsoup.parse(url, 5000);
    StringBuilder result = new StringBuilder();
    Elements listaCzynnikow = doc.select(".czynnik");
    String[] nazwyCzynnikow = new String[]{"Temperatura", "Wiatr"};
    for (Element czynnik : listaCzynnikow) {
      boolean nazwaOk = false;
      for (String nazwaCzynnika : nazwyCzynnikow)
        if (nazwaCzynnika.equals(czynnik.text()))
          nazwaOk = true;
      if (!nazwaOk)
        continue;
      String czynnikResult = czynnik.nextElementSibling().text();
      czynnikResult = czynnikResult.replace("[NESW]+ przy ", "");
      czynnikResult = czynnikResult.replace(" °C", "C");
      if (result.length() > 0)
        result.append(", ");
      result.append(czynnikResult);
    }
    return result.toString();
  }
}
