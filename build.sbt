name := "k8sop"

organization := "jp.co.septeni_original"

version := "0.1-SNAPSHOT"

resolvers ++= Seq(
  Resolver.bintrayRepo("digdag", "maven"),
  "jitpack" at "https://jitpack.io"
)

def digdagVersion = "0.9.27"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.0",
  "org.slf4j"                  % "slf4j-api"       % "1.7.25",
  "ch.qos.logback"             % "logback-classic" % "1.2.3" % Provided,

  // 以下のPRがリリースされるまではYamlクラスが正常に動作しないので、maven cenralではなくjitpackから読む。
  // https://github.com/kubernetes-client/java/pull/314
  //  "io.kubernetes"              % "client-java"     % "2.0.0",
  "com.github.kubernetes-client.java" % "client-java" % "9e23b710f1bf024ca691b3617cd20dc4dc2933e1",

  // provide by digdag-server or client.
  "io.digdag" % "digdag-spi"          % digdagVersion % Provided,
  "io.digdag" % "digdag-plugin-utils" % digdagVersion % Provided,

  // avoid this issue (https://stackoverflow.com/questions/23701209/sbt-0-13-2-m3-to-0-13-5-rc3-issue)
  // copied from https://github.com/resteasy/Resteasy/blob/3.0/resteasy-dependencies-bom/pom.xml
  "org.jboss.spec.javax.ws.rs"      % "jboss-jaxrs-api_2.0_spec"       % "1.0.0.Final" % Provided,
  "junit"                           % "junit"                          % "4.12"        % Provided,
  "javax.activation"                % "activation"                     % "1.1.1"       % Provided,
  "org.jboss.spec.javax.annotation" % "jboss-annotations-api_1.2_spec" % "1.0.0.Final" % Provided,
  "org.jboss.logging"               % "jboss-logging-annotations"      % "2.1.0.Final" % Provided,
  "org.jboss.logging"               % "jboss-logging-processor"        % "2.1.0.Final" % Provided,
  "org.jboss.spec.javax.servlet"    % "jboss-servlet-api_3.1_spec"     % "1.0.0.Final" % Provided,
  "commons-io"                      % "commons-io"                     % "2.5"         % Provided,
  "net.jcip"                        % "jcip-annotations"               % "1.0"         % Provided,
  "org.apache.httpcomponents"       % "httpclient"                     % "4.5.2"       % Provided
)

