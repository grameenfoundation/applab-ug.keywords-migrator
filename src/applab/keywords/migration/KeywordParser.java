package applab.keywords.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import com.sforce.soap.schemas._class.ImportBackendServerKeywords.MenuItemAdapter;

//import applab.Keyword;

public class KeywordParser {

    private String menu = "";
    SalesforceKeywordProxy salesforceKeywordProxy;
    private HashMap<String, List<Keyword>> keywordsMap = new HashMap<String, List<Keyword>>();
    
    public void updateSalesforceKeywords(String lastUpdateDate, String menuName) throws Exception {

        try {
            Configuration.init();
            Configuration.parseConfig();
        }
        catch (Exception e) {
            System.out.println("Failed to parse configuration");
            e.printStackTrace();
            System.exit(-1);
        }
        try {
            DatabaseHelpers.createConnection(Configuration.getConfiguration("databaseURL", ""),
                    Configuration.getConfiguration("databaseUsername", ""),
                    Configuration.getConfiguration("databasePassword", "")
                    );
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        try {
            ResultSet keywordResultSet = DatabaseHelpers.executeSelectQuery(getKeywordsQuery(lastUpdateDate));
            int resultSetSize = getResultSetSize(keywordResultSet);
            System.out.println(resultSetSize);
            if (resultSetSize > 0) {
                List<Keyword> updatedKeywords = new ArrayList<Keyword>();
                salesforceKeywordProxy = new SalesforceKeywordProxy();
                while (keywordResultSet.next()) {
                    Keyword keyword = new Keyword(keywordResultSet.getString("keyword.id"),
                            keywordResultSet.getString("category.name").trim().replaceAll("\\s+", " ") + " "
                                    + (keywordResultSet.getString("keyword.keyword").trim().replaceAll("\\s+", " ")),
                            keywordResultSet.getString("keyword.content"), keywordResultSet.getString("keyword.attribution"),
                            keywordResultSet.getInt("keyword.isDeleted"), keywordResultSet.getString("keyword.createDate"),
                            keywordResultSet.getString("keyword.updated"));                 
                            updatedKeywords.add(keyword);
                        if (updatedKeywords.size() >= 50) {
                            processAndSendKeywords(updatedKeywords, menuName);
                            System.out.println("Added " + updatedKeywords.size());
                            updatedKeywords = new ArrayList<Keyword>();
                            
                        }
                }
                if (updatedKeywords.size() > 0) {
                    processAndSendKeywords(updatedKeywords, menuName);
                }
                System.out.println("----Done migrating----");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void processAndSendKeywords(List<Keyword> keywords, String menuLabel) throws Exception {
        try {
            List<MenuItemAdapter> adapters = generateMenuItemAdapters(keywords);
            System.out.println("Fininshed processing adapters, sending keywords ...");
            salesforceKeywordProxy.sendKeywordsToSalesforce(adapters, menuLabel);
            System.out.println("----------------Finished processing -------------");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
       
    /*
     * Checks if the keyword already exists in list of adapters
     */
    private boolean existsInAdaptersList(List<MenuItemAdapter> adapters, String path) {
        for (MenuItemAdapter adapter : adapters) {
            if (adapter.getMenuPath().contentEquals(path)) {
                return true;
            }
        }
        return false;
    }
    
    private String buildAdapterMenuPath(String[] tokens, int level) {
        String path = "";
        for (int x = 0; x < tokens.length && x <= level; x++) {
            String y = tokens[x];
            path = path + " " + y;
        }
        return path;        
    }

    private List<MenuItemAdapter> generateMenuItemAdapters(List<Keyword> keywords) {
        List<MenuItemAdapter> adapters = new ArrayList<MenuItemAdapter>();
        for (int i = 0; i < keywords.size(); i++) {
            
            // split keywords breabcrumb to build menu paths for adapters
            keywords.get(i).setBreadcrumb(keywords.get(i).getBreadcrumb().trim().replaceAll("\\s+", " "));
            String[] rawTokens = keywords.get(i).getBreadcrumb().split(" ");
            String[] tokens = removeUnderscore(keywords.get(i).getBreadcrumb().split(" "));
            
            // current and previous paths
            String previousPath = "";
            String currentPath = "";
            // loop over all the tokens and build adapters
            for (int j = 0; j < tokens.length; j++) {
                previousPath = currentPath;
                currentPath = buildAdapterMenuPath(rawTokens, j);
                
                // Make sure that their is no same adapter
                if (!existsInAdaptersList(adapters, currentPath)) {
                    MenuItemAdapter adapter = new MenuItemAdapter();
                    adapter.setMenuPath(currentPath);
                    //adapter.setId(generateHashForId(currentPath));
                    adapter.setIsActive(keywords.get(i).isActive());
                    adapter.setLabel(tokens[j]);
                    adapter.setIsProcessed(false);
                    
                    System.out.println("Added adapter with label: " 
                            + adapter.getLabel() + " and menu path: -> "
                            + adapter.getMenuPath()
                            + " previous menu path "
                            + previousPath);
                    // Fill in content, attribution et al if its the end point item
                    if (j == tokens.length - 1) {
                        adapter.setContent(keywords.get(i).getContent());
                        adapter.setAttribution(keywords.get(i).getAttribution());
                    }
                    /*if (!previousPath.isEmpty()) {
                        MenuItemAdapter previousAdapter = findPreviousAdapter(previousPath, adapters);
                        if (previousAdapter != null) {
                            adapter.setPreviousItemPath(previousAdapter.getMenuPath());
                        }
                    }*/
                    adapter.setPreviousItemPath(previousPath);
                    adapters.add(adapter);
                }
            }
        }            
         return adapters;
    }
    
   /* private MenuItemAdapter findPreviousAdapter(String previousPath, List<MenuItemAdapter> adapters) {
        for (MenuItemAdapter adapter : adapters) {
            if (adapter.getMenuPath().equals(previousPath)) {
                return adapter;
            } 
        }
        return null;
    } */
     
    
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

    public String getKeywordsQuery(String lastUpdateDate) {
        StringBuilder commandText = new StringBuilder();
        commandText.append("SELECT ");
        commandText.append("keyword.id, ");
        commandText.append("keyword.keyword, ");
        commandText.append("keyword.weight, ");
        commandText.append("keyword.content, ");
        commandText.append("keyword.attribution, ");
        commandText.append("keyword.isDeleted, ");
        commandText.append("keyword.createDate, ");
        commandText.append("keyword.updated, ");
        commandText.append("category.name ");
        commandText.append("FROM ");
        commandText.append("keyword ");
        commandText.append("INNER JOIN ");
        commandText.append("category ");
        commandText.append("ON ");
        commandText.append("category.id = keyword.categoryId ");

        // commented this out since we want to pass wthose that have since been deactivated as well
        // commandText.append("AND ");
        // commandText.append("category.ckwsearch = 1 ");
        commandText.append("WHERE ");
        commandText.append("keyword.updated >= '");
        commandText.append(lastUpdateDate);
        commandText.append("' OR ");
        commandText.append("category.updated >= '");
        commandText.append(lastUpdateDate);
        commandText.append("' ");
        // commandText.append(" ORDER BY category.name ");
        System.out.println(commandText.toString());
        return commandText.toString();
    }
}
