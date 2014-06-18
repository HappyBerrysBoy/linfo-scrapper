package linfo.project.terminalscraping.scrapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import linfo.project.terminalscraping.scrapper.WebSite;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Scraper {
//	scrap 할 항목들이 정의된 파일
	private final String ITEM_FILE_NAME = "scrap-list.xml";
	
//	scrap 항목별 수집 terminal 목록
	private final String TERMINAL_LIST_FILE_NAME = "site-list.xml";
	
	private HashMap<String, String>items;
	ArrayList<WebSite> terminalList;
	private String filePath;
	private HashMap<Integer, String> params;
	
	
	public Scraper() throws IOException{
//		this.filePath = Scraper.class.getProtectionDomain().getCodeSource().getLocation().getPath() 
//				+ "/" + Scraper.class.getPackage().getName().replace(".", "/") + "/";
		this.filePath = new File(".").getCanonicalPath() + "/";
		
		items = new HashMap<>();
		params = new HashMap<>();
		terminalList = new ArrayList<>();
		this.setItems();
	}
	
	
	
	/**
	* ITEM_FILE_NAME 파일(xml)에 정의되어 있는 item 들을 가져와서 변수에 저장한다.
	*/
	private void setItems(){
		try{
			Document itemDoc = getXMLDocument(this.filePath + this.ITEM_FILE_NAME);;
			NodeList itemNode = itemDoc.getElementsByTagName("scrap");
			
			for(int i = 0; i < itemNode.getLength() ; i++){
				Element itemElement = (Element)itemNode.item(i);
				items.put(itemElement.getAttribute("item"), itemElement.getTextContent());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	
	public void addParam(int index, String value){
		this.params.put(index, value);
	}
	
	public ArrayList<String> getItems(){
		return getKeyList(this.items);
	}
	
	
	
	
	/**
	* TERMINAL_LIST_FILE_NAME 파일(xml)에 정의되어 있는 터미널 web site 정보를 변수에 저장한다.
	* @param item item 의 id 값
	*/
	private void setTerminalList(String item){
		try{
			Document terminalDoc = getXMLDocument(this.filePath + this.TERMINAL_LIST_FILE_NAME);
			NodeList itemNode = terminalDoc.getElementsByTagName(this.items.get(item));
			
			NodeList siteNode = ((Element)itemNode.item(0)).getElementsByTagName("site");
			
			
			for(int i = 0; i < siteNode.getLength() ; i++){
				Element siteElement = (Element)siteNode.item(i);
				WebSite t = new WebSite();
				t.setId(siteElement.getAttribute("id"));
				
				if(siteElement.getElementsByTagName("terminal").getLength() > 0){
					Element terminalElement = (Element)siteElement.getElementsByTagName("terminal").item(0);
					t.setTerminalId(terminalElement.getTextContent());
				}
				
				if(siteElement.getElementsByTagName("name").getLength() > 0){
					Element nameElement = (Element)siteElement.getElementsByTagName("name").item(0);
					t.setTerminalName(nameElement.getTextContent());
				}
				
				if(siteElement.getElementsByTagName("url").getLength() > 0){
					Element urlElement = (Element)siteElement.getElementsByTagName("url").item(0);
					t.setUrl(urlElement.getTextContent());
				}
				
				if(siteElement.getElementsByTagName("encoding").getLength() > 0){
					Element encodingElement = (Element)siteElement.getElementsByTagName("encoding").item(0);
					t.setEncoding(encodingElement.getTextContent());
				}
				
				if(siteElement.getElementsByTagName("cookie-url").getLength() > 0){
					Element encodingElement = (Element)siteElement.getElementsByTagName("cookie-url").item(0);
					t.setCookieUrl(encodingElement.getTextContent());
				}
				
				NodeList paramNode = siteElement.getElementsByTagName("param");
				for(int j = 0; j < paramNode.getLength() ; j++){
					Element paramElement = (Element)paramNode.item(j);
					String value = paramElement.getTextContent();
					if (value.contains("[SYSDATE(-)")){
						value = getDateAfter("-", value.replace("[SYSDATE(-)", "").replace("]", "").trim());
					}else if (value.contains("[SYSDATE(/)")){
						value = getDateAfter("/", value.replace("[SYSDATE(/)", "").replace("]", "").trim());
					}else if (value.contains("[SYSDATE.YEAR")){
						value = getDateAfter(Calendar.YEAR, value.replace("[SYSDATE.YEAR", "").replace("]", "").trim());
					}else if (value.contains("[SYSDATE.MONTH")){
						value = getDateAfter(Calendar.MONTH, value.replace("[SYSDATE.MONTH", "").replace("]", "").trim());
					}else if (value.contains("[SYSDATE.DAY_OF_MONTH")){
						value = getDateAfter(Calendar.DAY_OF_MONTH, value.replace("[SYSDATE.DAY_OF_MONTH", "").replace("]", "").trim());
					}else if (value.contains("[SYSDATE")){
						value = getDateAfter(value.replace("[SYSDATE", "").replace("]", "").trim());
					}
					
					if (value.contains("{[") && value.contains("]}")){
						String sIndex = value.replace("{[", "").replace("]}", "");
						value = this.params.get(Integer.parseInt(sIndex));
					}
					
					t.putParam(paramElement.getAttribute("key"), value);
				}
				
				NodeList requestPropertyNode = siteElement.getElementsByTagName("request-property");
				for(int j = 0; j < requestPropertyNode.getLength() ; j++){
					Element requestPropertyElement = (Element)requestPropertyNode.item(j);
					t.putRequestProperty(requestPropertyElement.getAttribute("key"), requestPropertyElement.getTextContent());
				}
				
				this.terminalList.add(t);
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	
	
	public ArrayList<WebSite> getTerminalList(String item){
		this.setTerminalList(item);
		return this.terminalList;
	}
	

		
	
	/*
	public StringBuffer getHtml(TerminalWebSite terminal){
		StringBuffer html = new StringBuffer();
		URL url;
		String urlParameters = this.mapToUrlParamString(terminal.getParam());
	    HttpURLConnection connection = null;  
	    String cookieHeader = "";
	    
	    try {
	    	if(terminal.getCookieUrl() != null && terminal.getCookieUrl().length() > 0){
	        	URL cookieUrl = new URL(terminal.getCookieUrl());
		    	HttpURLConnection cookieCon = (HttpURLConnection) cookieUrl.openConnection();
		    	cookieCon.connect();
		    	
		    	StringBuilder sb = new StringBuilder();

		    	// find the cookies in the response header from the first request
		    	List<String> cookies = cookieCon.getHeaderFields().get("Set-Cookie");
		    	if (cookies != null) {
		    	    for (String cookie : cookies) {
		    	        if (sb.length() > 0) {
		    	            sb.append("; ");
		    	        }

		    	        // only want the first part of the cookie header that has the value
		    	        String value = cookie.split(";")[0];
		    	        sb.append(value);
		    	    }
		    	}

		    	// build request cookie header to send on all subsequent requests
		    	cookieHeader = sb.toString();
		    	cookieCon.disconnect();
		    	
	        }
	    	
	    	url = new URL(terminal.getUrl());
	    	connection = (HttpURLConnection)url.openConnection();
	    	
	    	connection.setRequestMethod("POST");
	    	connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	        connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
	        
	        connection.setRequestProperty("Cookie", cookieHeader);
	        
	    	ArrayList<String> requestParameters = Util.getKeyList(terminal.getReqeustProperty());
	    	for(String requestParameter : requestParameters){
	    		connection.setRequestProperty(requestParameter, terminal.getReqeustProperty().get(requestParameter));
	    	}
	        
//	    	connection.setUseCaches(false);
	    	connection.setInstanceFollowRedirects(false);
	    	connection.setDoInput(true);
	    	connection.setDoOutput(true);
	    	connection.setConnectTimeout(15000);
	    	
	    	//Send request
	    	DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
	    	wr.writeBytes(urlParameters);
	    	wr.flush();
	    	wr.close();

	    	//Get Response	
	    	InputStream is = connection.getInputStream();
	    	BufferedReader rd = null;
	    	rd = new BufferedReader(new InputStreamReader(is, terminal.getEncoding()));
	    	
	    	
	    	String line;
	    	
	    	while((line = rd.readLine()) != null) {
	    		html.append(line);
	    		html.append('\r');
	    	}
	    	rd.close();

	    } catch (Exception e) {
	    	e.printStackTrace();
	    	return null;

	    } finally {
	    	if(connection != null) {
	    		connection.disconnect(); 
	    	}
	    }
	    
	    
		return html;
	}
	*/
	
	
	/**
	* 터미널 접속정보를 이용해 해당 페이지의 html 코드를 가져온다.
	* @param terminal Terminal Web Site 정보
	*/
	public String getHtml(WebSite terminal){
		String html = new String();
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
		try{
			if(terminal.getCookieUrl() != null && terminal.getCookieUrl().length() > 0){
				HttpGet cookieHttp = new HttpGet(terminal.getCookieUrl());
				CloseableHttpResponse cookieResponse = httpclient.execute(cookieHttp);
				HttpEntity cookieEntity = cookieResponse.getEntity();
			}
			
			HttpPost terminalHttp = new HttpPost(terminal.getUrl());
			
			List <NameValuePair> nvps = new ArrayList <NameValuePair>();
			ArrayList<String> parameters = getKeyList(terminal.getParam());
			for(String parameter : parameters){
				nvps.add(new BasicNameValuePair(parameter, terminal.getParam().get(parameter)));
			}
			terminalHttp.setEntity(new UrlEncodedFormEntity(nvps));
			ArrayList<String> requestParameters = getKeyList(terminal.getReqeustProperty());
	    	for(String requestParameter : requestParameters){
	    		terminalHttp.setHeader(requestParameter, terminal.getReqeustProperty().get(requestParameter));
	    	}
	    	
		    
		    CloseableHttpResponse response = httpclient.execute(terminalHttp);
		    HttpEntity entity = response.getEntity();
		    html = EntityUtils.toString(entity, terminal.getEncoding());
		    // do something useful with the response body
		    // and ensure it is fully consumed
		    EntityUtils.consume(entity);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try{
				httpclient.close();
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
				
		
		
		return html;
	}
	
	
	
	
	
	
	/**
	* 파라미터 맵을 파라미터 URL 형태로 변경한다.
	* e.g. days=3days&param=value
	* @param m (변수명, 값)으로 이루어진 HashMap 
	*/
	private String mapToUrlParamString(HashMap<String, String> m){
		String urlParameter = "";
		ArrayList<String> params = getKeyList(m);
		
		int counter = 0;
		
		for(String param : params){
			counter++;
			urlParameter += param + "=" + m.get(param);
			if(counter < params.size()){
				urlParameter += "&";
			}
		}
		
		return urlParameter;
	}
	
	
	
	/**
	* HashMap<String, String> 자료 구조로 부터 key 목록을 ArrayList<String> 형태로 추출 한다.
	* @param h <String, String> 타입의 HashMap
	* @return ArrayList<String> key 목록
	*/
	private ArrayList<String> getKeyList(HashMap<String, String> h){
		ArrayList<String> keyList = new ArrayList<>();
		
		Object[] keys = h.keySet().toArray();
		
		for(int i =0 ; i < keys.length ; i++){
			keyList.add(keys[i].toString());
		}
		
		return keyList;
	}
	
	
	/**
	* XML File로 부터 Document 객체를 생성한다.
	* @param file file 명
	* @return Document
	*/
	private Document getXMLDocument(String file){
		Document doc = null;
		
		try{
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			File xmlFile = new File(file);
			doc = docBuilder.parse(xmlFile);
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return doc;
	}
	
	
	/**
	* 며칠 후 의 날자를 반환한다
	* @return yyyymmdd
	*/
	private String getDateAfter(String amount){
		if (amount == null || amount.length() == 0) amount = "0";
		int intAmount = Integer.parseInt(amount);
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, intAmount);
		return String.format("%04d", c.get(Calendar.YEAR)) 
				+ String.format("%02d", c.get(Calendar.MONTH) + 1)
				+ String.format("%02d", c.get(Calendar.DAY_OF_MONTH));
	}
	
	
	
	/**
	* 며칠 후 의 날자 요소 (년, 월, 일)을 반환한다
	* @param field Calendar.YEAR 또는 Calendar.MONTH 또는 Calendar.DAY_OF_MONTH
	* @param amount 며칠 후 를 조회 할 것인가
	* @return yyyy 또는 mm 또는 dd
	*/
	private String getDateAfter(int field, String amount){
		if (amount == null || amount.length() == 0) amount = "0";
		int intAmount = Integer.parseInt(amount);
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, intAmount);
		
		if (field == Calendar.YEAR){
			return String.format("%04d", c.get(Calendar.YEAR));
		}else if (field == Calendar.MONTH){
			return String.format("%02d", c.get(Calendar.MONTH) + 1);
		}else if (field == Calendar.DAY_OF_MONTH){
			return String.format("%02d", c.get(Calendar.DAY_OF_MONTH));
		}else{
			return String.format("%04d", c.get(Calendar.YEAR)) 
					+ String.format("%02d", c.get(Calendar.MONTH) + 1)
					+ String.format("%02d", c.get(Calendar.DAY_OF_MONTH));
		}
		
	}
	
	
	
	/**
	* 며칠 후 의 날자를 반환한다
	* @param separator 년, 월, 일 구분자
	* @param amount 며칠 후 를 조회 할 것인가
	* @return yyyy (separator) mm (separator) dd 의 형태(separator로 년,월,일 을 구분함)
	*/
	private String getDateAfter(String separator, String amount){
		if (amount == null || amount.length() == 0) amount = "0";
		int intAmount = Integer.parseInt(amount);
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, intAmount);
		return String.format("%04d", c.get(Calendar.YEAR)) 
				+ separator + String.format("%02d", c.get(Calendar.MONTH) + 1)
				+ separator + String.format("%02d", c.get(Calendar.DAY_OF_MONTH));
	}
	
}
