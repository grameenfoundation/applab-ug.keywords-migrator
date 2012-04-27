package applab.keywords.migration;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import applab.keywords.migration.Configuration;

import com.sforce.soap.enterprise.LoginResult;
import com.sforce.soap.enterprise.QueryResult;
import com.sforce.soap.enterprise.SessionHeader;
import com.sforce.soap.enterprise.SforceServiceLocator;
import com.sforce.soap.enterprise.SoapBindingStub;
import com.sforce.soap.enterprise.fault.InvalidFieldFault;
import com.sforce.soap.enterprise.fault.InvalidIdFault;
import com.sforce.soap.enterprise.fault.InvalidQueryLocatorFault;
import com.sforce.soap.enterprise.fault.InvalidSObjectFault;
import com.sforce.soap.enterprise.fault.LoginFault;
import com.sforce.soap.enterprise.fault.MalformedQueryFault;
import com.sforce.soap.enterprise.fault.UnexpectedErrorFault;
import com.sforce.soap.enterprise.sobject.Menu_Item__c;

import applab.server.SalesforceProxy;

public class SalesforceKeywordProxy {

    private static SoapBindingStub binding;

    public static SoapBindingStub getBinding() {
		return binding;
	}
	public SalesforceKeywordProxy() throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException {
        initBinding();
    }
    public static void initBinding() throws InvalidIdFault, UnexpectedErrorFault, LoginFault, RemoteException, ServiceException {

        SforceServiceLocator serviceLocator = new SforceServiceLocator();
        serviceLocator.setSoapEndpointAddress(Configuration.getConfiguration("salesforceAddress", ""));
        binding = (SoapBindingStub)serviceLocator.getSoap();
        LoginResult loginResult = binding.login(
                Configuration.getConfiguration("salesforceUsername", ""),
                Configuration.getConfiguration("salesforcePassword", "")
                        + Configuration.getConfiguration("salesforceToken", ""));

        binding._setProperty(SoapBindingStub.ENDPOINT_ADDRESS_PROPERTY,
                loginResult.getServerUrl());

        SessionHeader sessionHeader = new SessionHeader(
                loginResult.getSessionId());
        binding.setHeader(serviceLocator.getServiceName().getNamespaceURI(),
                "SessionHeader", sessionHeader);
    }
/*    public SalesforceKeywordProxy() throws ServiceException, RemoteException {
        super();
    }*/




    public String getMenu (String menu) throws RemoteException {
        QueryResult query = binding.query("Select Id FROM Menu__c WHERE Label__c = '"+ menu + "'");
        return query.getRecords(0).getId();
    }

}
