name := "k8sop"

organization := "jp.co.septeni_original"

version := "0.1-SNAPSHOT"

resolvers ++= Seq(
  Resolver.bintrayRepo("digdag", "maven"),
  "jitpack" at "https://jitpack.io"
)

def digdagVersion = "0.10.5"

scalacOptions ++= Seq(
  "-Ywarn-unused-import"
)

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.5",
  "org.slf4j"                  % "slf4j-api"       % "2.0.12",
  "commons-io"                 % "commons-io"      % "2.15.1",
  "ch.qos.logback"             % "logback-classic" % "1.5.3" % Provided,
  "io.kubernetes"              % "client-java"     % "23.0.0-legacy",
  // provide by digdag-server or client.
  "io.digdag" % "digdag-spi"          % digdagVersion % Provided,
  "io.digdag" % "digdag-plugin-utils" % digdagVersion % Provided,
  // avoid this issue (https://stackoverflow.com/questions/23701209/sbt-0-13-2-m3-to-0-13-5-rc3-issue)
  // copied from https://github.com/resteasy/Resteasy/blob/3.0/resteasy-dependencies-bom/pom.xml
  "commons-io"                      % "commons-io"                     % "2.15.1"      % Provided,
  "org.jboss.spec.javax.ws.rs"      % "jboss-jaxrs-api_2.0_spec"       % "1.0.0.Final" % Provided,
  "junit"                           % "junit"                          % "4.13.2"      % Provided,
  "javax.activation"                % "activation"                     % "1.1.1"       % Provided,
  "org.jboss.spec.javax.annotation" % "jboss-annotations-api_1.2_spec" % "1.0.0.Final" % Provided,
  "org.jboss.logging"               % "jboss-logging-annotations"      % "2.1.0.Final" % Provided,
  "org.jboss.logging"               % "jboss-logging-processor"        % "2.1.0.Final" % Provided,
  "org.jboss.spec.javax.servlet"    % "jboss-servlet-api_3.1_spec"     % "1.0.0.Final" % Provided,
  "net.jcip"                        % "jcip-annotations"               % "1.0"         % Provided,
  "org.apache.httpcomponents"       % "httpclient"                     % "4.5.14"      % Provided
)
