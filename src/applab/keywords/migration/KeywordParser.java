package applab.keywords.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import com.sforce.soap.schemas._class.ImportBackendServerKeywords.MenuItemAdapter;

/**
 * Main class for loading and formaing keywords for migration Menu Item Adapters are built from keyowrds
 * 
 * @author GF
 * 
 */
public class KeywordParser {

    SalesforceKeywordProxy salesforceKeywordProxy;
    private Calendar calendar = Calendar.getInstance();

    /**
     * Entry method for updaet call. Orchestration of all activities happens here
     * 
     * @param lastUpdateDate
     * @param menuName
     */
    public void updateSalesforceKeywords(String lastUpdateDate, String menuName) {

        try {
            Configuration.getConfig().parseConfig();
            DatabaseHelpers.createConnection(Configuration.getConfig().getConfiguration("databaseURL", ""),
                    Configuration.getConfig().getConfiguration("databaseUsername", ""),
                    Configuration.getConfig().getConfiguration("databasePassword", "")
                    );
            ResultSet keywordResultSet = DatabaseHelpers.executeSelectQuery(getKeywordsQuery(lastUpdateDate));
            int resultSetSize = getResultSetSize(keywordResultSet);
            System.out.println(resultSetSize + " Keywords found");
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
                    if (updatedKeywords.size() >= 20) {
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
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Makes saleforce call to send menu item adapters for further processing in salesforce webservice
    private void processAndSendKeywords(List<Keyword> keywords, String menuLabel) throws Exception {
        try {
            List<MenuItemAdapter> adapters = generateMenuItemAdapters(keywords);
            System.out.println("Fininshed processing adapters, sending keywords ...");
            salesforceKeywordProxy.sendKeywordsToSalesforce(adapters, menuLabel);
            System.out.println("Finished processing....");
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
                    // adapter.setId(generateHashForId(currentPath));
                    adapter.setIsActive(keywords.get(i).isActive());
                    adapter.setLastModifiedDate(calendar);
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
                    adapter.setPreviousItemPath(previousPath);
                    adapters.add(adapter);
                }
            }
        }
        return adapters;
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
        commandText.append("WHERE ");
        commandText.append("category.ckwsearch = 1 AND (");
        commandText.append("keyword.updated >= '");
        commandText.append(lastUpdateDate);
        commandText.append("' OR ");
        commandText.append("category.updated >= '");
        commandText.append(lastUpdateDate);
        commandText.append("' )");
        System.out.println(commandText.toString());
        return commandText.toString();
    }
}
