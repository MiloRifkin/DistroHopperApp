import java.io.*;

public class ThreaderDownload implements Runnable{
    private long start;
    private long end;
    private HTTPMultiThreadDownloadFile httpInstance;
    private RandomAccessFile file;

    /**
     * Constructor Method
     * @param httpInstance
     * @param start
     * @param end
     * @param file
     */
    public ThreaderDownload(HTTPMultiThreadDownloadFile httpInstance, long start, long end, RandomAccessFile file){
        this.start = start;
        this.end = end;
        this.httpInstance = httpInstance;
        this.file = file;
    }

    /**
     * Method provides, entry point for thread and other logic 
     */
    public void run(){
        try{
            httpInstance.downloadRangeFile(start, end, file);
        } catch(IOException e){
           System.err.println("Download error in range " + start + "-" + end + ": " + e.getMessage()); 
        }
    }
}
