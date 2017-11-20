package com.feeyo.redis.engine.manage.stat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.feeyo.redis.engine.codec.RedisRequestPolicy;
import com.feeyo.redis.net.front.handler.CommandParse;
import com.feeyo.util.MailUtil;

public class MailNotification implements StatListener {
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd");
	
	//收件人
	private static final String[] addrs = new String[] {};
	private static final String[] fileNames = new String[] {"big_key.txt", "top_hundred.txt"};
	private static final String filePath = System.getProperty("FEEYO_HOME")+"\\store\\";
	
	
	
	private void createFile(String fileName, String content) {
		FileOutputStream fos = null;
		OutputStreamWriter out = null;
		BufferedWriter bw = null;
		try {
			File file =new File(fileName);
	        if(!file.exists()){  
	        	file.createNewFile();
	        } 
			fos = new FileOutputStream(file,false);
			out =new OutputStreamWriter(fos,"utf-8");
	        bw = new BufferedWriter(out);
	        bw.write(content);
	        bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	private String[] transFilePathName(String[] filenames) {
		String[] attachementNames = new String[filenames.length];
		for(int index = 0 ;index< filenames.length; index++) {
			attachementNames[index] = filePath + sdf.format(new Date())+"_"+filenames[index];
		}
		return attachementNames;
	}
	
	private void clearFiles( String[] attachmentsNames) {
		for(String fileName : attachmentsNames) {
			File file = new File(fileName);
			if(file.exists())
				file.delete();
		}
	}


	@Override
	public void onBigKey(String password, String cmd, String key, int requestSize, int responseSize) {
	}


	@Override
	public void onWatchType(String password, RedisRequestPolicy policy, String key, int requestSize) {
	}


	@Override
	public void onScheduleToZore() { 

		try {
			
			ConcurrentHashMap<String, BigKey> bigkeyStats = StatUtil.getBigKeyMap();
			StringBuffer tableBuffer = new StringBuffer();
			tableBuffer.append("#############   bigkey status   #################\n");
			tableBuffer.append("|    cmd    |     key     |    size    |    count    |");
			
			//获取bigkey的数据信息
			Collection<Entry<String, BigKey>> entrys = bigkeyStats.entrySet();
			for(Entry<String, BigKey> entry : entrys) {
				BigKey bigkey = entry.getValue();
				tableBuffer.append("\n");
				tableBuffer.append("|    ");
				tableBuffer.append(bigkey.cmd).append("    |    ");
				tableBuffer.append(bigkey.key).append("    |    ");
				tableBuffer.append(bigkey.size).append("    |    ");
				tableBuffer.append(bigkey.count).append("    |");
			}
			
			String[] attachmentsNames = transFilePathName(fileNames);
			createFile(attachmentsNames[0],tableBuffer.toString());
			tableBuffer.setLength(0);
			
			Set<Entry<String, CollectionKey>> collectionKeySet =  StatUtil.getCollectionKeySet();
			tableBuffer.append("#############   top  hundred   #################\n");
			tableBuffer.append("|    key    |     type     |    length    |    count_1k    |    count_10k    |");
			for(Entry<String, CollectionKey> entry : collectionKeySet) {
				CollectionKey collectionKey = entry.getValue();
				tableBuffer.append("\n");
				tableBuffer.append("|    ");
				tableBuffer.append(collectionKey.key).append("    |    ");
				String type = null;
				if (collectionKey.type == CommandParse.HASH_WATCH) {
					type = "hash";
				} else if (collectionKey.type == CommandParse.LIST_WATCH) {
					type = "list";
				} else if (collectionKey.type == CommandParse.SET_WATCH) {
					type = "set";
				} else if (collectionKey.type == CommandParse.SORTED_SET_WATCH) {
					type = "sortedset";
				}
				tableBuffer.append(type).append("    |    ");
				tableBuffer.append(collectionKey.length).append("    |    ");
				tableBuffer.append(collectionKey.count_1k).append("    |    ");
				tableBuffer.append(collectionKey.count_10k).append("    |    ");
			}
			
			createFile(attachmentsNames[1], tableBuffer.toString());
			
			MailUtil.send( addrs, " ## REDIS PROXY STATUS ##", "Please check the attachments", attachmentsNames);
			clearFiles(attachmentsNames);
			
		} catch(Exception e) {
			
		}
		
	}


	@Override
	public void onSchedulePeroid(int peroid) {
	}
}
