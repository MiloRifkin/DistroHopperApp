import java.io.*;

public class ThreaderDownload implements Runnable{
    private long start;
    private long end;
    private byte[] rangefiledata; //byte array to hold the downlaodewd data for the specified range.  used byte array as http repoonse for files is binary data.
    private HTTPMultiThreadDownloadFile httpInstance;

    /**
     * Constructor method
     * @param httpInstance
     * @param start
     * @param end
     */
    public ThreaderDownload(HTTPMultiThreadDownloadFile httpInstance, long start, long end){
        this.start = start;
        this.end = end;
        this.httpInstance = httpInstance;
    }

    /**
     * Method provides, entry point for thread and other logic 
    */
    public void run(){
        try{
            //Download the range bytes of file
            rangefiledata = httpInstance.downloadRangeFile(start, end);

        }catch(IOException e){
            //return e.getMessage(); or return empty data[]
            rangefiledata = new byte[0];
        }
    }

    /**
     * Getter method for rangefiledata.
     * @return
     */
    public byte[] getRangefiledata(){
        return rangefiledata;
    }

}
