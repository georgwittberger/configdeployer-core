<h1>ConfigDeployer Core Module</h1>
<p>ConfigDeployer is a Java framework designed for the deployment of software configurations in different installation environments using XML format profiles.</p>
<h2>Getting started</h2>
<h3>Obtaining the source code</h3>
<p>Install Git on your computer, open the Git Bash and navigate to the directory where you want to check-out the repository. Then execute the following command:</p>
<pre>git clone https://github.com/georgwittberger/configdeployer-core.git</pre>
<p>You will get a new sub-directory named "configdeployer-core" which contains the Maven project.</p>
<h3>Building the binaries</h3>
<p>Install Maven on your computer, open a terminal and navigate to the directory where the ConfigDeployer projects resides. Then execute the following command:</p>
<pre>mvn install</pre>
<p>You will get a new sub-directory named "target" which contains the binary JAR file "configdeployer-core-VERSION.jar". Note that <em>VERSION</em> is the version number you have downloaded.</p>
<h3>Integrating with your own project</h3>
<p>To use the ConfigDeployer framework in your own Java project you could...</p>
<ul>
  <li>Manually add the binary JAR file "configdeployer-core-VERSION.jar" to the classpath of your project.</li>
  <li>Install the artifact in your Maven repository and use it as a dependency in your POM:</li>
  <ol>
    <li>Open a terminal, navigate to the directory where the JAR file resides and execute the following command:
	<pre>mvn install:install-file -Dfile=configdeployer-core-VERSION.jar</pre></li>
	<li>Add the artifact as a dependency to your POM:
	<pre>&lt;dependency&gt;
  &lt;groupId&gt;com.configdeployer&lt;/groupId&gt;
  &lt;artifactId&gt;configdeployer-core&lt;/artifactId&gt;
  &lt;version&gt;VERSION&lt;/version&gt;
&lt;/dependency&gt;</pre></li>
  </ol>
</ul>
<p>Note that <em>VERSION</em> stands for the version number you want to use.</p>
<h2>Creating a profile</h2>
<p>There is a XML schema definition (XSD) you can use to create and validate a profile: <code>/src/main/xsd/config-profile.xsd</code>. Here is a simple properties file example:</p>
<pre>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
&lt;config-profile name=&quot;Example&quot; version=&quot;1.0&quot; xmlns=&quot;http://tsmms.com/utils/configdeployer/profile&quot;
    xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot;
    xsi:schemaLocation=&quot;http://tsmms.com/utils/configdeployer/profile config-profile.xsd &quot;&gt;
  &lt;properties-files&gt;
    &lt;properties-file location=&quot;/etc/myconfig.properties&quot;&gt;
      &lt;entry operation=&quot;SET&quot; key=&quot;my.property.key&quot; value=&quot;My new property value&quot;/&gt;
    &lt;/properties-file&gt;
  &lt;/properties-files&gt;
&lt;/config-profile&gt;</pre>
<p>This profile will change the value of the key <code>my.property.key</code> within the properties file <code>/etc/myconfig.properties</code> to the new value <code>"My new property value"</code> (or create this entry if it does not exist).</p>
<p>Please refer to the documentation for more details on the profile structure: <code>/src/main/site/index.html</code>.</p>
<h2>Deploying a profile</h2>
<p>Profile deployment is done with the <code>ProfileDeployer</code> class:</p>
<pre>InputStreamProvider inputStreamProvider = new FileInputStreamProvider(new File(&quot;myprofile.xml&quot;));
try {
  ProfileDeployer profileDeployer = new ProfileDeployer(new ProfileProvider(inputStreamProvider));
  boolean success = profileDeployer.deploy();
  // check if deployment was successful
} catch (DeployerException e) {
  // do some error handling
}</pre>
<p>You may use additional preparers which do the pre-processing before the profile is applied to the environment. Please refer to the documentation for more details on deployment: <code>/src/main/site/index.html</code>.</p>
