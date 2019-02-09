package it;

import deployment.mgmt.api.json.MgmtJsonApi;
import deployment.mgmt.factory.MgmtFactory;

import static java.lang.System.getProperties;

public class MgmtTest {
    public static void main(String[] args) {
//        System.getProperties().put("skipSnapshots", "true");
        getProperties().put("fakeStart", "true");

        MgmtFactory factory = new MgmtFactory();
        MgmtJsonApi jsonApi = factory.getMgmtJsonApi();
        jsonApi.newReleaseToServicesJson("config");
    }
}