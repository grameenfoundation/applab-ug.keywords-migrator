package applab.keywords.migration;

import java.rmi.RemoteException;
import java.util.List;

import javax.xml.rpc.ServiceException;

import applab.keywords.migration.Configuration;

import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.enterprise.SessionHeader;
import com.sforce.soap.enterprise.SforceServiceLocator;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;
import com.sforce.soap.schemas._class.ImportBackendServerKeywords.ImportBackendServerKeywordsBindingStub;
import com.sforce.soap.schemas._class.ImportBackendServerKeywords.ImportBackendServerKeywordsServiceLocator;
import com.sforce.soap.schemas._class.ImportBackendServerKeywords.MenuItemAdapter;

public class SalesforceKeywordProxy {

    private static ImportBackendServerKeywordsBindingStub serviceStub;

    public SalesforceKeywordProxy() throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException {
        serviceStub = initBinding();
    }

    /**
     * Initializes binding to saleforce webservice
     */
    private static ImportBackendServerKeywordsBindingStub initBinding() throws InvalidIdFault, UnexpectedErrorFault, LoginFault,
            RemoteException, ServiceException {

        ImportBackendServerKeywordsServiceLocator importKeywordsServiceLocator = new ImportBackendServerKeywordsServiceLocator();
        ImportBackendServerKeywordsBindingStub serviceStub = (ImportBackendServerKeywordsBindingStub)importKeywordsServiceLocator
                .getImportBackendServerKeywords();

        // Use soap api to login and get session info
        SforceServiceLocator soapServiceLocator = new SforceServiceLocator();
        soapServiceLocator.setSoapEndpointAddress(Configuration.getConfig().getConfiguration("salesforceAddress", ""));
        SoapBindingStub binding = (SoapBindingStub)soapServiceLocator.getSoap();
        LoginResult loginResult = binding.login(Configuration.getConfig().getConfiguration("salesforceUsername", ""),
                Configuration.getConfig().getConfiguration("salesforcePassword", "")
                        + Configuration.getConfig().getConfiguration("salesforceToken", ""));
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
