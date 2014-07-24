package edu.usc.pil.nlputils.plugins.preprocessor.process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import snowballstemmer.EnglishStemmer;
import snowballstemmer.PorterStemmer;
import snowballstemmer.SnowballStemmer;
import snowballstemmer.GermanStemmer;

public class Preprocess {
	private boolean doLowercase = false;
	private boolean doStemming = false;
	private boolean doStopWords = false;
	private boolean doLangDetect = false;
	private String delimiters = " .,;'\"!-()[]{}:?";
	private String[] inputFiles;
	private String stopwordsFile;
	private String outputPath;
	private String suffix;
	private HashSet<String> stopWordsSet = new HashSet<String>();
	SnowballStemmer stemmer=null;
	String stemLang;
	
	
	public Preprocess(String[] inputFiles, String stopwordsFile, String outputPath, String suffix, String delimiters, boolean doLowercase, boolean doStemming, String stemLang){
		this.inputFiles = inputFiles;
		this.stopwordsFile = stopwordsFile;
		this.outputPath = outputPath;
		this.suffix = suffix;
		this.delimiters=delimiters;
		this.doLowercase = doLowercase;
		this.doStemming = doStemming;
		this.stemLang = stemLang.toUpperCase();
	}
	
	public int doPreprocess() throws IOException, LangDetectException{
		
		if (stopwordsFile != null && !stopwordsFile.isEmpty()){
		// If stopwordsFile is not given, doStopWords is false by default. Check only if it's not empty
		File sFile = new File(stopwordsFile);
		if (!sFile.exists() || sFile.isDirectory()){
			System.out.println("Error in stopwords file path "+sFile.getAbsolutePath());
			appendLog("Error in stopwords file path "+sFile.getAbsolutePath());
			return -2;
		} else {
			doStopWords = true;
			String currentLine;
			BufferedReader br = new BufferedReader(new FileReader(sFile));
			while ((currentLine = br.readLine())!=null){
				stopWordsSet.add(currentLine.trim().toLowerCase());
			}
			br.close();
		}
		}
		
		if (doStemming){	// If stemming has to be performed, find the appropriate stemmer.
		if (stemLang.equals("AUTO DETECT LANGUAGE")){
			appendLog("Initializing Language Detection...");
			doLangDetect = true;
			System.out.println(System.getProperty("user.dir"));
			System.out.println(this.getClass().getResource("").getPath());
//			URL main = Preprocess.class.getResource("Preprocess.class");
//			if (!"file".equalsIgnoreCase(main.getProtocol()))
//			  throw new IllegalStateException("Main class is not stored in a file.");
//			File path = new File(main.getPath());
//			System.out.println(path);
			try{
			DetectorFactory.loadProfile("C:\\Users\\45W1N\\NLPUtils-application\\edu.usc.pil.nlputils.plugins.preprocessor\\profiles");
			} catch (com.cybozu.labs.langdetect.LangDetectException ex){
				ex.printStackTrace();
				System.out.println(ex.getCode());
			}
		} else{
			doLangDetect = false;
			switch(stemLang){
			case "EN":
				appendLog("Language set to English");
				stemmer = new EnglishStemmer();
				break;
			case "DE":
				appendLog("Language set to German");
				stemmer = new GermanStemmer();
				break;
			}
		}
		}
		
		for (String inputFile:inputFiles){
			System.out.println("Preprocessing file "+inputFile);
			appendLog("Preprocessing file "+inputFile);
			File iFile = new File(inputFile);
			if (!iFile.exists() || iFile.isDirectory()){
				System.out.println("Error in input file path "+iFile.getAbsolutePath());
				appendLog("Error in input file path "+iFile.getAbsolutePath());
				return -1;
			}
			
			int dindex = inputFile.lastIndexOf('.');
			int findex = inputFile.lastIndexOf('\\');
			String fname = inputFile.substring(findex,dindex);
			String ext = inputFile.substring(dindex+1);
			File oFile = new File(outputPath+"\\"+fname+suffix+"."+ext);
			System.out.println("Creating out file "+oFile.getAbsolutePath());
			appendLog("Creating out file "+oFile.getAbsolutePath());
			
			// doLangDetect only if doStemming is true
			if (doLangDetect) {
				stemmer = findLangStemmer(iFile);
				if (stemmer==null){
					appendLog("Failed to detect the language. Please select manually.");
					return -3;
				}
			}
			
			BufferedReader br = new BufferedReader(new FileReader(iFile));
			BufferedWriter bw = new BufferedWriter(new FileWriter(oFile));
			
			String currentLine;
			while((currentLine = br.readLine())!=null){
				//System.out.println(currentLine);
				for (char c:delimiters.toCharArray())
					currentLine = currentLine.replace(c, ' ');
				if (doLowercase)
					currentLine = currentLine.toLowerCase();
				if (doStopWords)
					currentLine = removeStopWords(currentLine);
				if (doStemming)
					currentLine = stem(currentLine);
				bw.write(currentLine);
				bw.newLine();
			}
			
			br.close();
			bw.close();
		}
		return 1;
	}

	private SnowballStemmer findLangStemmer(File iFile) throws IOException, LangDetectException {
		BufferedReader br = new BufferedReader(new FileReader(iFile));
		String sampleText="";
		for (int i = 0;i<3;i++){
			String currentLine = br.readLine();
			if (currentLine == null)
				break;
			sampleText = sampleText+ currentLine.trim().replace('\n', ' ');
		}
		Detector detector = DetectorFactory.create();
		detector.append(sampleText);
		String lang = detector.detect();
		switch(lang.toUpperCase()){
		case "EN":
			appendLog("Detected Language - English.");
			return new EnglishStemmer();
		case "DE":
			appendLog("Detected Language - German.");
			return new GermanStemmer();
		}
		return null;
	}

	private String stem(String currentLine) {
		StringBuilder returnString = new StringBuilder();
		String[] wordArray = currentLine.split("\\s+");
		for (String word: wordArray){
			stemmer.setCurrent(word);
			String stemmedWord = "";
			if(stemmer.stem())
				 stemmedWord = stemmer.getCurrent();
			if (!stemmedWord.equals(""))
				word = stemmedWord;
			returnString.append(word);
			returnString.append(' ');
		}
		return returnString.toString();
	}

	public String removeStopWords(String currentLine){
		StringBuilder returnString = new StringBuilder();
		String[] wordArray = currentLine.split("\\s+");
		for (String word : wordArray){
			if (!stopWordsSet.contains(word.toLowerCase())){
				returnString.append(word);
				returnString.append(' ');
			}
		}
		return returnString.toString();
	}
	
	// This function updates the consoleMessage parameter of the context.
		@Inject IEclipseContext context;
		private void appendLog(String message){
			IEclipseContext parent = context.getParent();
			parent.set("consoleMessage", message);
		}
}
