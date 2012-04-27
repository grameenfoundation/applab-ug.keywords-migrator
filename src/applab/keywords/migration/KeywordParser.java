package applab.keywords.migration;

import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.xml.rpc.ServiceException;
import com.sforce.soap.enterprise.Error;
import com.sforce.soap.enterprise.SaveResult;
import com.sforce.soap.enterprise.fault.InvalidFieldFault;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.InvalidSObjectFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;
import com.sforce.soap.enterprise.sobject.Menu_Item__c;

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
			DatabaseHelpers.createConnection(
					Configuration.getConfiguration("databaseURL", ""),
					Configuration.getConfiguration("databaseUsername", ""),
					Configuration.getConfiguration("databasePassword", ""));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		try {
			ResultSet keywordResultSet = DatabaseHelpers
					.executeSelectQuery(getKeywordsQuery());
			int resultSetSize = getResultSetSize(keywordResultSet);
			if (resultSetSize > 0) {
				List<Keyword> keywords = new ArrayList<Keyword>();
				while (keywordResultSet.next()) {
					Keyword keyword = new Keyword(
							keywordResultSet.getString("keyword.id"),
							keywordResultSet.getString("category.name").trim()
									.replaceAll("\\s+", " ")
									+ " "
									+ (keywordResultSet.getString(
											"keyword.keyword").trim()
											.replaceAll("\\s+", " ")),
							keywordResultSet.getString("keyword.content"));

					keywords.add(keyword);
				}
				createListOfSameLevelMenuItems(keywords);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Given some keywords like Animals Chicken Diseases, Crops
	 * Inorganic_Farming Coffee (1) the idea is to create a list at each level
	 * for example, at level 0, we would have Animals and Crops level 1 would be
	 * Chicken and Inorganic_Farming, level 2 would be Diseases and Coffee
	 * 
	 * this method takes a list of Keyword objects and creates a list of lists
	 * of menu items this list of lists is then saved to Salesforce where the
	 * keywords are represented as hierarchical menu items
	 * 
	 * @param variable
	 *            keywords
	 */
	private void createListOfSameLevelMenuItems(List<Keyword> keywords)
			throws InvalidSObjectFault, InvalidFieldFault, InvalidIdFault,
			UnexpectedErrorFault, RemoteException {
		int numberOfLevels = getNumberOfLevels(keywords);
		System.out.println("We have " + numberOfLevels + " levels");
		List<List<MenuItem>> levelsList = new ArrayList<List<MenuItem>>();
		for (int level = 0; level < numberOfLevels; level++) {
			levelsList.add(processLevel(keywords, level));
		}
		saveToSalesforce(levelsList);
		System.out.println("Saving Complete!!!!");
	}

	/**
	 * Given a level, which is any number from zero upwards this method creates
	 * a List of all menu items at that level
	 * 
	 * @param - variable keywords the list of keywords
	 * @param - variable level the level to be created
	 * @return- the list of menu items at the specified level
	 */
	private List<MenuItem> processLevel(List<Keyword> keywords, int level) {
		MenuItem menuItem = null;
		List<MenuItem> singleLevelMenuItemList = new ArrayList<MenuItem>();
		for (int i = 0; i < keywords.size(); i++) {
			String[] tokens = keywords.get(i).getLabel().trim()
					.replaceAll("\\s+", " ").split(" ");
			tokens = removeUnderscore(tokens);
			if (level < tokens.length) {
				menuItem = new MenuItem();
				menuItem.setMenuItemPath(createPathToMenuItem(tokens, level));
				menuItem.setMenuItem(tokens[level]);
				// check if this is the last level, if so, check that there is
				// content to be attached to this menu item
				if (level == tokens.length - 1 && level != 0
						&& null != keywords.get(i).getContent()) {
					menuItem.setContent(keywords.get(i).getContent());
				}
				// we do not want to add the same menu item twice
				if (!singleLevelMenuItemList.contains(menuItem)) {
					singleLevelMenuItemList.add(menuItem);
				}
			}
		}
		return singleLevelMenuItemList;
	}

	private void saveToSalesforce(List<List<MenuItem>> levelsList)
			throws InvalidSObjectFault, InvalidFieldFault, InvalidIdFault,
			UnexpectedErrorFault, RemoteException {
		List<Menu_Item__c> menuItemListToSave = new ArrayList<Menu_Item__c>();
		// the hashmap that will contain a MenuItem object and its Salesforce
		// Id.
		HashMap<MenuItem, String> parentIdHashMap = new HashMap<MenuItem, String>();

		for (int count = 0; count < levelsList.size(); count++) {
			List<MenuItem> itemList = levelsList.get(count);
			menuItemListToSave.clear();
			for (MenuItem menuItemKey : itemList) {
				Menu_Item__c menuItem = new Menu_Item__c();
				menuItem.setLabel__c(menuItemKey.getMenuItem());
				if (null != menuItemKey.getContent()) {
					menuItem.setContent__c(menuItemKey.getContent());
				}
				menuItem.setMenu__c(menu);
				menuItemListToSave.add(menuItem);
			}
			int numberOfBatches = 1;

			List<Menu_Item__c> listA = new ArrayList<Menu_Item__c>();
			for (int x = 0; x < menuItemListToSave.size(); numberOfBatches++) {
				for (int y = 0; y < 200 && x < menuItemListToSave.size(); x++) {
					listA.add(menuItemListToSave.get(y
							+ ((numberOfBatches - 1) * 200)));
					y++;
				}
				// do the save to SF
				SaveResult[] saveResult = SalesforceKeywordProxy.getBinding()
						.create(listA.toArray(new Menu_Item__c[0]));

				System.out.println("we have " + saveResult.length
						+ " Ids from SF");
				System.out.println("save result array follows");
				printArray(saveResult);

				for (int index = 0; index < saveResult.length; index++) {
					parentIdHashMap.put(
							(MenuItem) itemList.get(index
									+ ((numberOfBatches - 1) * 200)),
							saveResult[index].getId());
					if (!saveResult[index].isSuccess()) {
						com.sforce.soap.enterprise.Error[] errors = saveResult[index]
								.getErrors();
						printErrors(errors);
					}
				}
				System.out.println(parentIdHashMap);
				listA.clear();
			}
		}
	}

	/**
	 * Output any error messages we get during the save to salesforce
	 * 
	 * @param errors
	 *            - the array of error objects from salesforce
	 */
	private void printErrors(Error[] errors) {
		for (int i = 0; i < errors.length; i++) {
			System.out.println(errors[i].getMessage());
		}
	}

	/**
	 * Given a list of keywords, this method returns the highest number of
	 * levels that the longest keyword in the list contains
	 * 
	 * @param - variable keywords the list of keywords
	 */
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

	/**
	 * Given a level, which is any number from zero upwards this method creates
	 * a unique identifier which is actually the path up to the current level of
	 * that keyword
	 * 
	 * @param tokens
	 *            - an array whose elements represent the individual words right
	 *            up to the word in the current level of the keyword
	 * @param level
	 *            - the level to be created
	 * @return - the path to that particular token (menu item)
	 */
	private String createPathToMenuItem(String[] tokens, int level) {
		String menuItemPath = "";
		for (int x = 0; x < tokens.length && x < level + 1; x++) {
			String y = tokens[x];
			menuItemPath = menuItemPath + " " + y;
		}
		return menuItemPath;
	}

	/**
	 * Removes the underscore from the keywords
	 * 
	 * @param tokens
	 *            - an array of the space delimited keywords
	 * @return - the same array with the underscore removed
	 */
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

			if (currentRow > 0) {
				keywordResultSet.absolute(currentRow);
			} else {
				keywordResultSet.beforeFirst();
			}
		} catch (SQLException e) {
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
		// commandText.append("AND category.name = 'Farm_Inputs'");
		// commandText.append(" AND keyword.keyword LIKE 'Organic%'");
		commandText.append(" ORDER BY category.name ");
		System.out.println(commandText.toString());
		return commandText.toString();
	}

	/**
	 * Prints the list of ids returned from salesforce after saving
	 * 
	 * @param array
	 */
	private void printArray(SaveResult[] array) {
		String forFile = "";
		for (int i = 0; i < array.length; i++) {
			System.out.print(array[i].getId() + " ");
			forFile = forFile + " " + array[i].getId() + " ";
		}
	}
}
