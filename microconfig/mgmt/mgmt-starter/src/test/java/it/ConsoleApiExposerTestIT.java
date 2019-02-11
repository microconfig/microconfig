package it;

import deployment.console.ConsoleApiExposer;
import deployment.console.ConsoleApiExposerImpl;
import deployment.mgmt.api.console.Mgmt;
import deployment.mgmt.api.json.MgmtJsonApi;
import deployment.mgmt.factory.MgmtFactory;

import java.util.List;
import java.util.stream.Collectors;

import static deployment.console.ConsoleApiExposerImpl.*;
import static java.util.stream.Collectors.toList;

public class ConsoleApiExposerTestIT {
    public static void main(String[] args) {
        MgmtFactory factory = new MgmtFactory();
//        ConsoleApiExposer exposer = expose(factory.getMgmt(), factory.getMgmtJsonApi());
        ConsoleApiExposer exposer = expose(Mgmt.class, MgmtJsonApi.class);
        List<String> names = exposer.getCommandNames().collect(toList());
        System.out.println(names);
//        exposer.invoke("executeRemotely cr-test cr_reports mgmt status".split(" "));
//        exposer.invoke("st1atus".split(" "));
    }
}
