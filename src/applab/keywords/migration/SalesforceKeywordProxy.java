package applab.keywords.migration;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import applab.keywords.migration.Configuration;
import applab.server.ApplabConfiguration;
import applab.server.WebAppId;

import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.SaveResult;
import com.sforce.soap.enterprise.SessionHeader;
import com.sforce.soap.enterprise.SforceServiceLocator;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;
import com.sforce.soap.enterprise.sobject.Menu_Item__c;
import com.sforce.soap.schemas._class.FarmerCache.FarmerCacheBindingStub;
import com.sforce.soap.schemas._class.FarmerCache.FarmerCacheServiceLocator;
import com.sforce.soap.schemas._class.ImportBackendServerKeywords.ImportBackendServerKeywordsBindingStub;
import com.sforce.soap.schemas._class.ImportBackendServerKeywords.ImportBackendServerKeywordsServiceLocator;
import com.sforce.soap.schemas._class.ImportBackendServerKeywords.MenuItemAdapter;

public class SalesforceKeywordProxy {

    private static ImportBackendServerKeywordsBindingStub serviceStub;

    public SalesforceKeywordProxy() throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException {
        serviceStub = initBinding();
    }

    private static ImportBackendServerKeywordsBindingStub initBinding() throws InvalidIdFault, UnexpectedErrorFault, LoginFault,
            RemoteException, ServiceException {

        ImportBackendServerKeywordsServiceLocator importKeywordsServiceLocator = new ImportBackendServerKeywordsServiceLocator();
        ImportBackendServerKeywordsBindingStub serviceStub = (ImportBackendServerKeywordsBindingStub)importKeywordsServiceLocator
                .getImportBackendServerKeywords();

        // Use soap api to login and get session info
        SforceServiceLocator soapServiceLocator = new SforceServiceLocator();
        soapServiceLocator.setSoapEndpointAddress(Configuration.getConfiguration("salesforceAddress", ""));
        SoapBindingStub binding = (SoapBindingStub)soapServiceLocator.getSoap();
        LoginResult loginResult = binding.login(Configuration.getConfiguration("salesforceUsername", ""),
                Configuration.getConfiguration("salesforcePassword", "")
                        + Configuration.getConfiguration("salesforceToken", ""));
        SessionHeader sessionHeader = new SessionHeader(loginResult.getSessionId());

        // Share the session info with our webservice
        serviceStub.setHeader("http://soap.sforce.com/schemas/class/ImportBackendServerKeywords", "SessionHeader", sessionHeader);
        return serviceStub;
    }

    public boolean sendKeywordsToSalesforce(List<MenuItemAdapter> adapters, String menuLabel) throws Exception {

        MenuItemAdapter[] adapterArray = adapters.toArray(new MenuItemAdapter[adapters.size()]);
        return serviceStub.importBackendKeywords(adapterArray, menuLabel);

    }

}
