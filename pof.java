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
            <scope>compile</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>compile</scope>
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
            <plugin>
             <groupId>org.apache.maven.plugins</groupId>
             <artifactId>maven-shade-plugin</artifactId>
             <version>3.5.0</version>
             <executions>
                 <execution>
                     <phase>package</phase>
                     <goals>
                         <goal>shade</goal>
                     </goals>
                     <configuration>
                         <createDependencyReducedPom>false</createDependencyReducedPom>
                         <transformers>
                             <!-- Merge META-INF/services files, useful if you have SPI -->
                             <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                             <!-- Merge META-INF/spring.factories if needed -->
                             <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                 <resource>META-INF/spring.factories</resource>
                             </transformer>
                             <!-- Merge Manifest if you want to specify Main-Class -->
                             <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                 <mainClass>com.example.model.MainClassIfAny</mainClass> <!-- optional -->
                             </transformer>
                         </transformers>
                     </configuration>
                 </execution>
             </executions>
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
