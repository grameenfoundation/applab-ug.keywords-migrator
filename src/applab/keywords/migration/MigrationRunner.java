package applab.keywords.migration;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.xml.rpc.ServiceException;

public class MigrationRunner {

    public static void main(String[] args) throws ServiceException, IOException {
        try {
            Configuration.getConfig().parseConfig();
            KeywordParser keywordParser = new KeywordParser();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar date = Calendar.getInstance();
            String changePeriod = Configuration.getConfig().getConfiguration("changePeriod", "25");
            date.add(Calendar.HOUR_OF_DAY, Integer.parseInt("-" + changePeriod));
            String version = dateFormat.format(date.getTime());
            keywordParser.updateSalesforceKeywords(version, "CKW Search");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
