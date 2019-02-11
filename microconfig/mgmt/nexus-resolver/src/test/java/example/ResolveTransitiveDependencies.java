package example;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.util.List;

/**
 * Resolves the transitive (compile) dependencies of an artifact.
 */
public class ResolveTransitiveDependencies {

    /**
     * Main.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args)
            throws Exception {
        System.out.println("------------------------------------------------------------");
        System.out.println(ResolveTransitiveDependencies.class.getSimpleName());

        RepositorySystem system = Booter.newRepositorySystem();


        RepositorySystemSession session = Booter.newRepositorySystemSession(system);

        Artifact artifact = new DefaultArtifact("ru.sbt.risk.tradehub:th-server:TH-18.24-SNAPSHOT");
//        Artifact artifact = new DefaultArtifact("ru.sbt.cr.astreya.stresstest:stresstest-reports:RP-18.24-SNAPSHOT");


        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
        collectRequest.setRepositories(Booter.newRepositories(system, session));

        DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME, JavaScopes.COMPILE);
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);

        List<ArtifactResult> artifactResults =
                system.resolveDependencies(session, dependencyRequest).getArtifactResults();

//        for (ArtifactResult artifactResult : artifactResults) {
//            System.out.println(artifactResult.getArtifact() + " resolved to "
//                    + artifactResult.getArtifact().getFile());
//        }

        System.out.println(artifactResults.size());
    }

}
