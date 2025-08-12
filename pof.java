<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
           http://maven.apache.org/POM/4.0.0 
           https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>coherence-models</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Oracle Coherence Models</name>
    <description>POF model classes with Lombok for Oracle Coherence 14.1.1</description>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>  <!-- Change to your Java version -->
        <maven.compiler.target>11</maven.compiler.target>
        <lombok.version>1.18.26</lombok.version>
        <coherence.version>14.1.1.0.0</coherence.version>
    </properties>

    <dependencies>
        <!-- Oracle Coherence runtime API -->
        <dependency>
            <groupId>com.oracle.coherence</groupId>
            <artifactId>coherence</artifactId>
            <version>${coherence.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Compiler plugin with annotation processor for Lombok -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.10.1</version>
                <configuration>
                    <source>${maven.compiler.source}</source>
                    <target>${maven.compiler.target}</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>

package com.example.model;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Person implements PortableObject {

    private String name;
    private int age;

    @Override
    public void readExternal(PofReader in) throws IOException {
        name = in.readString(0);
        age = in.readInt(1);
    }

    @Override
    public void writeExternal(PofWriter out) throws IOException {
        out.writeString(0, name);
        out.writeInt(1, age);
    }
}


package com.example.model;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Animal implements PortableObject {

    private String species;
    private int legs;

    @Override
    public void readExternal(PofReader in) throws IOException {
        species = in.readString(0);
        legs = in.readInt(1);
    }

    @Override
    public void writeExternal(PofWriter out) throws IOException {
        out.writeString(0, species);
        out.writeInt(1, legs);
    }
}


------------------ on the server ----------------
  Keep override as it is
  create the pof and the cache config 

<?xml version="1.0"?>
<cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config
              coherence-cache-config.xsd">
  <caching-scheme-mapping>
    <!-- Map cache names to schemes -->
    <cache-mapping>
      <cache-name>people</cache-name>
      <scheme-name>distributed</scheme-name>
    </cache-mapping>
    <cache-mapping>
      <cache-name>animals</cache-name>
      <scheme-name>distributed</scheme-name>
    </cache-mapping>
  </caching-scheme-mapping>

  <caching-schemes>
    <!-- Distributed cache scheme (default settings) -->
    <distributed-scheme>
      <scheme-name>distributed</scheme-name>
      <service-name>DistributedCache</service-name>
      <backing-map-scheme>
        <local-scheme/>
      </backing-map-scheme>
      <autostart>true</autostart>
    </distributed-scheme>
    
    <!-- Enable POF globally -->
    <serializer>
      <instance>
        <class-name>com.tangosol.io.pof.ConfigurablePofContext</class-name>
        <init-params>
          <init-param>
            <param-type>String</param-type>
            <param-value>pof-config.xml</param-value> <!-- Path to POF config -->
          </init-param>
        </init-params>
      </instance>
    </serializer>
  </caching-schemes>
</cache-config>

<?xml version="1.0"?>
<!DOCTYPE pof-config SYSTEM "pof-config.dtd">
<pof-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="http://xmlns.oracle.com/coherence/coherence-pof-config"
            xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-pof-config
            coherence-pof-config.xsd">
  <user-type-list>
    <!-- Assign unique type IDs (>= 1000) for each class -->
    <user-type>
      <type-id>1001</type-id>
      <class-name>com.example.model.Person</class-name>
    </user-type>
    <user-type>
      <type-id>1002</type-id>
      <class-name>com.example.model.Animal</class-name>
    </user-type>
  </user-type-list>
</pof-config>







package junkpof;

import com.example.model.Person;
import com.example.model.Animal;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

public class CoherenceExtendClient {

    public static void main(String[] args) {
        // Tell Coherence where the configs are
        System.setProperty("tangosol.coherence.cacheconfig", "cache-config.xml");
        System.setProperty("tangosol.pof.enabled", "true");
        System.setProperty("tangosol.pof.config", "pof-config.xml");

        // Get a remote cache
        NamedCache<String, Object> cache = CacheFactory.getCache("test-cache");

        // Put Person
        Person person = new Person("Alice", 30);
        cache.put("person:1", person);

        // Put Animal
        Animal animal = new Animal("Cat", 4);
        cache.put("animal:1", animal);

        // Get and print values
        System.out.println("From cache: " + cache.get("person:1"));
        System.out.println("From cache: " + cache.get("animal:1"));

        CacheFactory.shutdown();
    }
}
SafeConfigurablePofContext

<!-- Allow unknown types -->
    <allow-interfaces>true</allow-interfaces>
    <allow-subclasses>true</allow-subclasses>
