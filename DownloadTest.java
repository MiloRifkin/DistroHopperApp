public class DownloadTest {
    /**
     * Method to call shit cause im to fucking tired for it
     * @param args
     */
    public static void main(String[] args) {
        try {
            String url = "https://mirrors.cicku.me/linuxmint/iso/stable/22.1/linuxmint-22.1-cinnamon-64bit.iso";
            HTTPMultiThreadDownloadFile downloader = new HTTPMultiThreadDownloadFile(url);
            downloader.multiThreadingDownload("m23.iso");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
//https://releases.ubuntu.com/24.04.2/ubuntu-24.04.2-desktop-amd64.iso                      -> unable to ever work, also corrupted or read time out
//https://mirrors.cicku.me/linuxmint/iso/stable/22.1/linuxmint-22.1-cinnamon-64bit.iso      -> more often than not, file is accessible and on occasion readtime out with file or corupted file.