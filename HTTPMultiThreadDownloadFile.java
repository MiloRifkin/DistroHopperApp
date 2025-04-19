/*************************************
 *  Filename:  HTTPInteraction.java
 ***********************************/

import java.net.*;
import java.io.*;
import java.util.*;
import javax.net.ssl.*;		//library for https

/**Class HTTPInteractionMultiThreadingCopy */
public class HTTPMultiThreadDownloadFile {
	private String host;
	private String path;
	private String requestMessage;
	
		
	private static final int HTTP_PORT = 80;
	private static final int HTTPS_PORT = 443;	//HTTPS port
	private static final String CRLF = "\r\n";
	private static final int BUF_SIZE = 4096; 
	private static final int READ_TIMEOUT = 180000; 			//180sec
	private static final int CONNECTION_TIMEOUT = 90000;	//90sec
	boolean multithreadSup = false;		//Flag to check if multithreading is accepted in server
	private boolean transferEncodingFlag = false;
	private boolean httpsFlag = false;	//HTTPS Flag
	
	/**
	 * Create a HTTPInteraction object
	 * @param url
	 */
	public HTTPMultiThreadDownloadFile(String url) {
		//check if http or https
		if(url.startsWith("https://")){
			httpsFlag = true;	//update https flag
			url = url.substring(8);	//removes the https:// part
		}else if (url.startsWith("http://")){
			httpsFlag = false;
			url = url.substring(7);
		}else{
			httpsFlag = false;	//default. 
		}
		
        String[] array = url.split("/",2);
        this.host = array[0];

        if (array.length > 1){
            this.path = "/" + array[1];   
        }
        else{
            this.path = "/";
        }

		this.requestMessage = ("GET " + this.path + " HTTP/1.1" + CRLF + "Host: " + this.host + CRLF + "Connection: close" + CRLF + CRLF);
	}	
	
	/**
	 * Creates socket connection based on http/https
	 * @return
	 * @throws IOException
	 */
	private Socket createSocket() throws IOException{
		//Set up socket connection	for https or http + Deal with timeout handling
		Socket connection;
		if (httpsFlag == true){
			SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();	//Create https connection
			connection = factory.createSocket();

			SocketAddress socketAddress = new InetSocketAddress(host, HTTPS_PORT);	//Create a socket address
			connection.setSoTimeout(READ_TIMEOUT);									//Set read time out
			connection.connect(socketAddress, CONNECTION_TIMEOUT);					//Connect connection with connection timeout
		}else{																				//Create http connection
			connection = new Socket();

			SocketAddress socketAddress= new InetSocketAddress(host, HTTP_PORT);	//Create a socket address
			connection.setSoTimeout(READ_TIMEOUT);									//Set read time out
			connection.connect(socketAddress, CONNECTION_TIMEOUT);					//Connect connection with connection timeout
		}
		return connection;

    }
		
	/**
	 * Send Http request, get content length, transfer encodind and accept-ranges
	 * @return
	 * @throws IOException
	 */
	public long processHeaders() throws IOException {
		Socket connection;
		connection = createSocket();

		BufferedReader fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));	//Assign the input and output streams to connection
		DataOutputStream toServer = new DataOutputStream(connection.getOutputStream());

		toServer.writeBytes(requestMessage);	//Send requestMessage to http server
		
		//Read the status line from response message. use split for more reliability
		String statusLine = fromServer.readLine(); 
		if (statusLine == null || statusLine.isEmpty()){
			throw new IOException("Error: Empty response");
		}

		String[] statusSplits =  statusLine.split(" ");
		if (statusSplits.length < 2){
			throw new IOException("Error: response not supported");
		}

		String statusCode = statusSplits[1];
		if(!statusCode.equals("200") && !statusCode.equals("206")){ //206: range request
			throw new IOException("Error: Response status code:" + statusCode);
		}

		//Check headers.
		boolean loop = false;
		String newLine = "";
		String headers = "";
		long contentLength = -1;

		while (loop == false){
			newLine = fromServer.readLine();
			if (newLine.isEmpty()){
				loop = true;
			}
			else{
				headers += newLine + CRLF;
				if ((newLine.toLowerCase()).startsWith("content-length:")){
					contentLength = Long.parseLong(newLine.substring(15).trim());
				}
				else if ((newLine.toLowerCase()).startsWith("transfer-encoding:") && newLine.contains("chunked") ){
					transferEncodingFlag = true;
				}
				else if((newLine.toLowerCase().startsWith("accept-ranges:"))){
					if (newLine.toLowerCase().contains("bytes")){
						multithreadSup = true;
					}
				}
			}
		}
		//DEBUG: could print out headers to debug
		
		//close connections
		fromServer.close();
		toServer.close();
		connection.close();

		return contentLength; //transferEncodingFlag and multithreadSup flags are global and dont need to be returned.
	}


	/**
	 * Method downloads file
	 * @param fileName
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void multiThreadingDownload(String fileName) throws IOException, InterruptedException{
		long contentlength = processHeaders();	//get contentlength

		//check for transfer encoding
		if (transferEncodingFlag){
			byte[] data = transferEncoding();	//Get data in bytes[]
			try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
				outputStream.write(data);	//Write data to file
			} 
			return;
		}
		if (contentlength <= 0){
			throw new IOException("ERROR: invalid/missing content length");
		}
		
		int numOfThreads; //gets chunks of about 1GB

		//Check if multithread is supported or size is small (1GB)
		if(!multithreadSup || contentlength < 1073741824 ){
			numOfThreads = 1;	//Set threads to 1, so downloads in one go
		}else{
			numOfThreads = (int) contentlength / 1073741824;	
			if (contentlength % 1073741824 != 0){
				numOfThreads += 1;	//left over bytes, add 1 extra thread to deal with it
			}
		}
		//Double check
		if (numOfThreads <= 0 ){
			numOfThreads = 1;
		}

		long individualThreadSize = contentlength / numOfThreads;	//Holds size of individual threads. All same size.

		//double check
		if (individualThreadSize == 0){
			numOfThreads = 1;
			individualThreadSize = contentlength;
		}

		//Arrays to hold threads and downloads
		ThreaderDownload[] downloads = new ThreaderDownload[numOfThreads];
		Thread[] threads = new Thread[numOfThreads];
		
		try(RandomAccessFile file = new RandomAccessFile(fileName, "rw")){
			file.setLength(contentlength);	//Set file size
			//Insilise threads
			for (int i = 0; i < numOfThreads; i++){
				long start = i*individualThreadSize;
				long end;

				if (i == numOfThreads -1){
					end = contentlength -1;
				}else{
					end = start + individualThreadSize -1;
				}

				//DEBUG
				//System.out.println("Thread " + i + " downloading from " + start + " to " + end);

				//Create downloads + threads
				downloads[i] = new ThreaderDownload(this, start, end, file);
				threads[i] = new Thread(downloads[i]);
				threads[i].start();				
			}
			for(Thread thread : threads){
				thread.join();
			}
		}
	}

	/**
	 * Modified verision of sendrequest for downloading specified range of bytes of file
	 * @param start
	 * @param end
	 * @param file
	 * @throws IOException
	 */
	public void downloadRangeFile(long start, long end, RandomAccessFile file) throws IOException{
		Socket connection = createSocket();
		DataOutputStream toServer = new DataOutputStream(connection.getOutputStream());
		InputStream fromServer = connection.getInputStream();
		
		//Adjust message for range-accept
		String rangeRequestMessage = ("GET " + this.path + " HTTP/1.1" + CRLF + "Host: " + this.host + CRLF + "Range: bytes=" + start+ "-" + end + CRLF + "Connection: close" + CRLF + CRLF);
		toServer.writeBytes(rangeRequestMessage);

		//iterate through the headers till body.
		BufferedReader headers = new BufferedReader(new InputStreamReader(fromServer));
		String newline;
		while ((newline = headers.readLine()) != null && !newline.isEmpty()) {
		}
		file.seek(start);
		byte[] buffer = new byte[BUF_SIZE];
		int bytesRead;
		while((bytesRead = fromServer.read(buffer)) != -1){
			file.write(buffer, 0, bytesRead);
		}

		//close connections
		fromServer.close();
		toServer.close();
		//headers.close();
		connection.close();
	}




	/**
	 * TRANSFER ENCODING FUNCTION. Private, want the class to only call this and not be accesible outside
	 * @return
	 * @throws IOException
	 */
	private byte[] transferEncoding() throws IOException{
		//char[] body = new char[MAX_OBJECT_SIZE];	//Array of body lines
		//String octetSize;
		int decimalChunkSize = 0;
		//int bodylength = 0;	//?-1 originally done as that??
		//boolean continue = true;

		Socket connection = createSocket();
		DataOutputStream toServer = new DataOutputStream(connection.getOutputStream());
		InputStream fromServer = connection.getInputStream();

		toServer.writeBytes(requestMessage);

		//iterate through the headers till body.
		BufferedReader headers = new BufferedReader(new InputStreamReader(fromServer));
		String newline;
		while ((newline = headers.readLine()) != null && !newline.isEmpty()) {}

		ByteArrayOutputStream contentstream = new ByteArrayOutputStream();
		BufferedReader chunkReader = new BufferedReader(new InputStreamReader(fromServer));	//Bufferedreader for reading chunk sizes
		
	
		String chunkSize = "";
		byte[] chunkData = new byte[decimalChunkSize];
		int bytesRead = 0;

		//chunk starts with num of octets (bytes) of data expressed as hexidecimal
		//e.g. The chunk size 11\r\n (in hexadecimal) = chunk of data followed = 11 hexadecimal bytes.
		//iterate through body till empty using encoding transfer.
		while (true){
			try{
				//read chunksize line
				chunkSize = chunkReader.readLine();
			
				//check if empty 
				if (chunkSize == null || chunkSize.trim().isEmpty()){	//use trim() to get rid of heading and leading white spaces
					break;
				}
				
				//convert chunkline from hex to decimal
				decimalChunkSize = Integer.parseInt(chunkSize.trim(), 16);
	
				//0 size chunk = end of response
				if (decimalChunkSize == 0){
					break;
				}
				
				// Read number of bytes of data into buffer
				chunkData = new byte[decimalChunkSize];
				bytesRead = 0;
				while(bytesRead < decimalChunkSize){
					//read.() returns amount of characters read.
					int read = fromServer.read(chunkData, bytesRead, decimalChunkSize - bytesRead);
					if (read == -1){
						break;
					}
					bytesRead += read;
					
				}
				contentstream.write(chunkData, 0, bytesRead);	//write bytes
				chunkReader.readLine();	//Discard the CRLF after each chunk.
		
			}catch (IOException e){
				throw new IOException("ERROR: Transfer encoding error");
			}	
		}
		toServer.close();
		fromServer.close();
		connection.close();
		chunkReader.close();
		return contentstream.toByteArray();
	}
}