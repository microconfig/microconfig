package it;

import deployment.mgmt.factory.MgmtFactory;

import java.util.Properties;

import static deployment.mgmt.init.InitParams.*;
import static java.lang.System.getProperties;

public class InitTest {
    public static void main(String[] args) {
        Properties properties = getProperties();
//        properties.put("skipConfigFetch", "true");

        properties.put(ENV, "cr-dev6");
        properties.put(GROUP, "cr_util_services");
        properties.put(CONFIG_GIT_URL, "https://16805899@sbtatlas.sigma.sbrf.ru/stash/scm/rp/config.git");
        properties.put(CONFIG_BRANCH_OR_TAG, "CR_INFRA-18.25.2");
        properties.put(PROJECT_FULL_VERSION_OR_POSTFIX, ".2");
        properties.put(NEXUS_CREDENTIALS, ":");
        properties.put(CONFIG_SOURCE, "GIT");
        properties.put(NEXUS_RELEASE_REPOSITORY, "http://172.30.162.1/nexus/content/repositories/releases");
//        properties.put(NEXUS_RELEASE_REPOSITORY, "http://nexus.sigma.sbrf.ru:8099/nexus/content/repositories/RiskPlatform_release");

        new MgmtFactory().getMgmt().fullInit();
    }
}