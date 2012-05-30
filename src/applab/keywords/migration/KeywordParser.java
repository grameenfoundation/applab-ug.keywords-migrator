package applab.keywords.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.xml.rpc.ServiceException;
import com.sforce.soap.enterprise.Error;
import com.sforce.soap.enterprise.SaveResult;
import com.sforce.soap.enterprise.fault.InvalidFieldFault;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.InvalidSObjectFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;
import com.sforce.soap.enterprise.sobject.Attachment;
import com.sforce.soap.enterprise.sobject.Menu_Item__c;
//import applab.Keyword;

public class KeywordParser {

    private String menu = "";
    private String filePath;

    public KeywordParser() throws RemoteException, ServiceException {
        
        Configuration.init();
        if (this.filePath != null) {
            Configuration.setFilePath(this.filePath);
        }
        try {
            Configuration.parseConfig();
        } catch (Exception e) {
            System.out.println("Failed to parse configuration");
            e.printStackTrace();
            System.exit(-1);
        }
        SalesforceKeywordProxy salesforceKeywordProxy = new SalesforceKeywordProxy();
        menu = salesforceKeywordProxy.getMenu("CKW Search");
        try {
            DatabaseHelpers.createConnection(Configuration.getConfiguration("databaseURL", ""),
                    Configuration.getConfiguration("databaseUsername", ""),
                    Configuration.getConfiguration("databasePassword", "")
            );
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        try {
            ResultSet keywordResultSet = DatabaseHelpers.executeSelectQuery(getKeywordsQuery());
            int resultSetSize = getResultSetSize(keywordResultSet);
            System.out.println(resultSetSize);
            if (resultSetSize > 0) {
                List<Keyword> keywords = new ArrayList<Keyword>();
                while (keywordResultSet.next()) {
                        Keyword keyword = new Keyword(keywordResultSet.getString("keyword.id"), 
                                keywordResultSet.getString("category.name").trim().replaceAll("\\s+", " ") + " " +(keywordResultSet.getString("keyword.keyword").trim().replaceAll("\\s+", " ")),
                                keywordResultSet.getString("keyword.content"), keywordResultSet.getString("keyword.attribution"));

                        keywords.add(keyword);
                }
                processing(keywords);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void processing(List<Keyword> keywords) throws InvalidSObjectFault, InvalidFieldFault, InvalidIdFault, UnexpectedErrorFault, RemoteException {
        int numberOfLevels = getNumberOfLevels(keywords);
        System.out.println("We have "+numberOfLevels + " levels");
        List<List<MenuItem>> levelsList = new ArrayList<List<MenuItem>>();
        for (int level = 0; level < numberOfLevels; level++ ) {
            levelsList.add(processLevel(keywords, level));
        }
        try {
			saveToSalesforce(levelsList);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		}
        System.out.println("SAVING COMPLETE!!!!");
    }

    private void saveToSalesforce(List<List<MenuItem>> levelsList) throws IOException, ServiceException {
        //finally save!!!
        List<Menu_Item__c> menuItemListToSave = new ArrayList<Menu_Item__c>();
        HashMap<MenuItem, String> parentIdHashMap = new HashMap<MenuItem, String>();

        for (int count = 0; count < levelsList.size(); count++) {
            System.out.println("level " + count);
            List<MenuItem> itemList = levelsList.get(count);
            menuItemListToSave.clear();
            System.out.println("level " + count + " has " + itemList.size() + " items");
            for (MenuItem menuItemKey : itemList) {
                  System.out.println("map contents " + parentIdHashMap);
                Menu_Item__c menuItem = new Menu_Item__c();
                menuItem.setLabel__c(menuItemKey.getMenuItem());
                menuItem.setLast_Modified_Date__c(menuItemKey.getLastModifiedDate());
                System.out.println("PARENT = " + parentIdHashMap.get(menuItemKey.getParentMenuItem()));
                menuItem.setParent_Item__c(parentIdHashMap.get(menuItemKey.getParentMenuItem()));
                if (null != menuItemKey.getContent()) {
                    menuItem.setContent__c(menuItemKey.getContent());
                    menuItem.setAttribution__c(menuItemKey.getAttribution());
                }
               menuItem.setMenu__c(menu);
               menuItemListToSave.add(menuItem);
            }
                int numberOfBatches = 1;

                List<Menu_Item__c> listA = new ArrayList<Menu_Item__c>();
                for (int x = 0; x < menuItemListToSave.size(); numberOfBatches++) {
                    for (int y = 0; y < 200 && x < menuItemListToSave.size(); x++) {
                        listA.add(menuItemListToSave.get(y+ ((numberOfBatches - 1) * 200)));
                        y++;
                    }
                    //do the save to SF
                    SaveResult[] saveResult = SalesforceKeywordProxy.getBinding().create(listA.toArray(new Menu_Item__c[0]));
                    System.out.println("we have "+saveResult.length + " Ids from SF");
                    System.out.println("save result array follows");
                    printArray(saveResult);
                    for (int index = 0; index < saveResult.length; index++) {
                        parentIdHashMap.put((MenuItem)itemList.get(index + (200 * (numberOfBatches - 1))), saveResult[index].getId());
                        if (!saveResult[index].isSuccess()) {
                            com.sforce.soap.enterprise.Error[] errors = saveResult[index].getErrors();
                            printErrors(errors);
                        }
                    }
                    System.out.println("hashmap contents after level "+ count);
                    System.out.println(parentIdHashMap);
                    listA.clear();
                }
        }
        saveImages(parentIdHashMap);
    }

    private void printErrors(Error[] errors) {
        for (int i = 0; i < errors.length; i++) {
            System.out .println(errors[i].getMessage());
        }
	}

	private List<MenuItem> processLevel(List<Keyword> keywords, int level) {
		 MenuItem menuItem = null;
	        List<MenuItem> singleLevelMenuItemList = new ArrayList<MenuItem>(); 
	        for (int i = 0; i < keywords.size(); i++) {
	            String [] tokens = keywords.get(i).getLabel().trim().replaceAll("\\s+", " ").split(" ");
	            tokens = removeUnderscore(tokens);
	            if (level < tokens.length) {
	                menuItem = new MenuItem();
	                menuItem.setTieBreaker(doTieBreak(tokens, level));
	                menuItem.setMenuItem(tokens[level]);

	                if (level == tokens.length - 1 && level != 0 && null != keywords.get(i).getContent()) {
	                    menuItem.setContent(keywords.get(i).getContent());
	                    menuItem.setParentMenuItem(new MenuItem(tokens[level - 1], tokens[level-2], "", doTieBreak(tokens, level-1)));
	                    menuItem.setAttribution(keywords.get(i).getAttribution());
	                }
	                else if (level != 0 && level != tokens.length - 1){
	                    if (level == 1) {
	                        menuItem.setParentMenuItem(new MenuItem(tokens[level - 1], "", "", doTieBreak(tokens, level -1)));
	                    }
	                    else {
	                        menuItem.setParentMenuItem(new MenuItem(tokens[level - 1], tokens[level-2], "", doTieBreak(tokens, level -1)));
	                    }
	                }
	                if (level == 0 && singleLevelMenuItemList.size() == 0) {
	                    singleLevelMenuItemList.add(menuItem);
	                }
	                if (!singleLevelMenuItemList.contains(menuItem)) {
	                    singleLevelMenuItemList.add(menuItem);
	                }  
	            }
	        }
	        return singleLevelMenuItemList;
    }

    private int getNumberOfLevels(List<Keyword> keywords) {
        int levels = 0;
        for (int i = 0; i < keywords.size(); i++) {
            int level = keywords.get(i).getLabel().split(" ").length;
            if (level > levels) {
                levels = level;
            }
        }
        return levels;
    }

    private String doTieBreak(String[] tokens, int level) {
        String tieBreaker = "";
        for (int x = 0; x < tokens.length && x < level + 1; x++) {
            String y = tokens[x];
            tieBreaker = tieBreaker + " " + y;
        }
        return tieBreaker;
    }
    private String[] removeUnderscore(String[] tokens) {
        if (tokens.length > 0) {
            for (int x = 0; x < tokens.length; x++) {
                String token = tokens[x].trim().replaceAll("_", " ");
                tokens[x] = token;
            }
        }
        return tokens;
    }

    private int getResultSetSize(ResultSet keywordResultSet) {
        int size = -1;
        int currentRow;

        try {
            currentRow = keywordResultSet.getRow();
            keywordResultSet.last();
            size = keywordResultSet.getRow();

            if(currentRow > 0) {
                keywordResultSet.absolute(currentRow);
            } 
            else {
                keywordResultSet.beforeFirst();
            }
        }
        catch (SQLException e) {
            return size;
        }

        return size;
    }

    private String getKeywordsQuery() {
        StringBuilder commandText = new StringBuilder();
        commandText.append("SELECT ");
        commandText.append("keyword.id, ");
        commandText.append("keyword.keyword, ");
        commandText.append("keyword.weight, ");
        commandText.append("keyword.content, ");
        commandText.append("keyword.attribution, ");
        commandText.append("keyword.updated, ");
        commandText.append("category.name ");
        commandText.append("FROM ");
        commandText.append("keyword ");
        commandText.append("INNER JOIN ");
        commandText.append("category ");
        commandText.append("ON ");
        commandText.append("category.id = keyword.categoryId ");
        commandText.append("AND ");
        commandText.append("category.ckwsearch = 1 ");
        commandText.append("AND ");
        commandText.append("NOT keyword.isDeleted ");
        commandText.append("AND category.name = 'MobileMoney_Directory'");
        //commandText.append(" ORDER BY category.name ");
        System.out.println(commandText.toString());
        return commandText.toString();
    }

    private void printArray(SaveResult[] array) {
        String forFile = "";
        for (int i =0; i < array.length; i ++) {
            System.out.print(array[i].getId() + " ");
            forFile = forFile + " " + array[i].getId() + " ";
        }
        System.out.println("................................................................................................");
    }

    private void checkForImages(MenuItem menuItem, HashMap<String, File> images, HashMap<MenuItem, String> parentIdHashMap) throws IOException, ServiceException {
        for (String imageFile : images.keySet()) {
    		if (imageFile.substring(0, imageFile.lastIndexOf(".")).equalsIgnoreCase(menuItem.getTieBreaker())) {
    			Attachment image = new Attachment();
				InputStream is = new FileInputStream(images.get(imageFile));
				long length = images.get(imageFile).length();
				// Create the byte array to hold the data
			    byte[] bytes = new byte[(int)length];

			    // Read in the bytes
			    int offset = 0;
			    int numRead = 0;
			    while (offset < bytes.length
			           && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
			        offset += numRead;
			    }
			    is.close();
			    image.setBody(bytes);
			    image.setParentId(parentIdHashMap.get(menuItem));
			    image.setName(imageFile);
			    SaveResult[] saveResult = SalesforceKeywordProxy.getBinding().create(new Attachment[] {image});
                for (int index = 0; index < saveResult.length; index++) {
                    if (!saveResult[index].isSuccess()) {
                        com.sforce.soap.enterprise.Error[] errors = saveResult[index].getErrors();
                        printErrors(errors);
                    }
                }
            }
        }
    }

    private void saveImages(HashMap<MenuItem, String> parentIdHashMap) throws IOException, ServiceException {
        HashMap<String, File> images = getImages();
        Set<MenuItem> menuItems = parentIdHashMap.keySet();
        for (MenuItem menuItem : menuItems) {
            checkForImages(menuItem, images, parentIdHashMap);
        }
    }

	private HashMap<String, File> getImages() {
		HashMap<String, File> images = new HashMap<String, File>();
		File imagesDirectory = new File("C:\\search.images");
		File[] children = imagesDirectory.listFiles();
		System.out.println("we have " + children.length + " images");
		for (File file : children) {
			images.put(" "+file.getName().trim().replaceAll("_", " "), file);
		}
		return images;
	}
}
