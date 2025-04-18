public class DownloadTest {
    public static void main(String[] args) {
        try {
            String url = "https://mirrors.cicku.me/linuxmint/iso/stable/22.1/linuxmint-22.1-cinnamon-64bit.iso";
            HTTPMultiThreadDownloadFile downloader = new HTTPMultiThreadDownloadFile(url);
            downloader.multiThreadingDownload("debian-12.5.0-amd64-netinst.iso");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
