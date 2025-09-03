<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.10.1</version>
            <configuration>
                <source>1.8</source>
                <target>1.8</target>
                <annotationProcessorPaths>
                    <!-- MapStruct processor -->
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>1.5.5.Final</version>
                    </path>
                    
                    <!-- Optional: Lombok annotation processor -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.28</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>

<dependencies>
    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.5.5.Final</version>
    </dependency>

    <!-- Optional: Lombok for getters/setters if you use it -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.28</version>
        <scope>provided</scope>
    </dependency>
</dependencies>


import java.util.List;

public interface GenericMapper<S, T> {

    // Forward mapping: External → MySystem
    T toMySystem(S source);

    // Reverse mapping: MySystem → External
    S toExternalSystem(T target);

    // List mapping forward
    List<T> toMySystemList(List<S> sourceList);

    // List mapping reverse
    List<S> toExternalSystemList(List<T> targetList);

    // Null-safe helpers
    default T toMySystemOrNull(S source) {
        if (source == null) return null;
        return toMySystem(source);
    }

    default S toExternalSystemOrNull(T target) {
        if (target == null) return null;
        return toExternalSystem(target);
    }
}

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper
public interface AnimalMapper extends GenericMapper<Animal, MyAnimal> {

    AnimalMapper INSTANCE = Mappers.getMapper(AnimalMapper.class);

    @Mapping(source = "name", target = "animalName")
    @Mapping(source = "age", target = "animalAge")
    @Mapping(source = "type", target = "species")
    @Override
    MyAnimal toMySystem(Animal animal);

    @InheritInverseConfiguration
    @Override
    Animal toExternalSystem(MyAnimal myAnimal);

    @Override
    List<MyAnimal> toMySystemList(List<Animal> animals);

    @Override
    List<Animal> toExternalSystemList(List<MyAnimal> myAnimals);
}

Animal dog = new Animal("Buddy", 5, "Dog");

// Forward
MyAnimal myDog = AnimalMapper.INSTANCE.toMySystem(dog);

// Reverse
Animal originalDog = AnimalMapper.INSTANCE.toExternalSystem(myDog);

// Null-safe
MyAnimal myNull = AnimalMapper.INSTANCE.toMySystemOrNull(null);

// List mapping
List<MyAnimal> myAnimals = AnimalMapper.INSTANCE.toMySystemList(Arrays.asList(dog));
