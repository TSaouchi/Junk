import os

# === Configuration ===
project_name = "my-multi-module-project"
modules = ["module-a", "module-b", "module-c"]
orchestrator = "orchestrator"
group_id = "com.example"
version = "1.0.0"

spring_boot_version = "3.2.0"
java_version = "17"

# === Utility Functions ===

def create_dir(path):
    os.makedirs(path, exist_ok=True)

def write_file(path, content):
    with open(path, "w") as f:
        f.write(content)

# === Maven POM Generators ===

def generate_parent_pom():
    modules_str = "\n".join([f"        <module>{m}</module>" for m in modules + [orchestrator]])
    return f"""<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>{group_id}</groupId>
    <artifactId>{project_name}</artifactId>
    <version>{version}</version>
    <packaging>pom</packaging>

    <modules>
{modules_str}
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>{spring_boot_version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <properties>
        <java.version>{java_version}</java.version>
        <skipTests>true</skipTests>
    </properties>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>{spring_boot_version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
"""

def generate_module_pom(name):
    return f"""<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>{group_id}</groupId>
        <artifactId>{project_name}</artifactId>
        <version>{version}</version>
    </parent>

    <artifactId>{name}</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
"""

def generate_orchestrator_pom():
    module_deps = "\n".join([f"""        <dependency>
            <groupId>{group_id}</groupId>
            <artifactId>{m}</artifactId>
            <version>{version}</version>
        </dependency>""" for m in modules])
    return f"""<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>{group_id}</groupId>
        <artifactId>{project_name}</artifactId>
        <version>{version}</version>
    </parent>

    <artifactId>{orchestrator}</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

{module_deps}
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
"""

# === Java Code Generators ===

def generate_main_class(package, class_name, run_app=True, scan_packages=None, with_profile=False):
    if scan_packages:
        scan_str = '@SpringBootApplication(scanBasePackages = {' + ', '.join([f'"{p}"' for p in scan_packages]) + '})'
    else:
        scan_str = "@SpringBootApplication"

    profile_annotation = "@Profile(\"standalone\")" if with_profile else ""

    return f"""package {package};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
{"import org.springframework.context.annotation.Profile;" if with_profile else ""}

{scan_str}
{profile_annotation}
public class {class_name} {{

    public static void main(String[] args) {{
        {"SpringApplication.run(" + class_name + ".class, args);" if run_app else "// Used as library only"}
    }}
}}
"""

def generate_service_class(package, module_name):
    class_name = module_name.title().replace("-", "") + "PingPlugin"
    return f"""package {package};

import org.springframework.stereotype.Component;

@Component
public class {class_name} {{
    public String getInfo() {{
        return "Info from {module_name}";
    }}
}}
"""

def generate_test_class(package, module_name):
    class_name = module_name.replace('-', '').title() 
    return f"""package {package};

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = {class_name}Application.class,
    properties = "spring.profiles.active=standalone"
)
class {class_name}ApplicationTests {{

    @Test
    void contextLoads() {{
    }}
}}
"""

# === Standard Structure Creator ===

def create_standard_structure(base_path, package, app_class_name, run_app=True, scan_packages=None, is_module=False, with_profile=False):
    main_java = f"{base_path}/src/main/java/{package.replace('.', '/')}"
    main_resources = f"{base_path}/src/main/resources"
    test_java = f"{base_path}/src/test/java/{package.replace('.', '/')}"
    test_resources = f"{base_path}/src/test/resources"

    for path in [main_java, main_resources, test_java, test_resources]:
        create_dir(path)

    write_file(f"{main_resources}/application.properties", "# Spring Boot config\n")

    write_file(
        f"{main_java}/{app_class_name}.java",
        generate_main_class(package, app_class_name, run_app=run_app, scan_packages=scan_packages, with_profile=with_profile)
    )

    if is_module:
        # Generate service class with module name from folder basename
        module_name = os.path.basename(base_path)
        write_file(f"{main_java}/{module_name.title().replace('-', '')}PingPlugin.java", generate_service_class(package, module_name))

    write_file(
        f"{test_java}/{app_class_name}Tests.java",
        generate_test_class(package, os.path.basename(base_path))
    )

# === Main Project Generator ===

# Create base project
create_dir(project_name)
write_file(f"{project_name}/pom.xml", generate_parent_pom())

# Create each module with @Profile("standalone") on main app class
for module in modules:
    path = f"{project_name}/{module}"
    package = f"{group_id}.{module.replace('-', '')}"
    app_class = module.replace('-', '').title() + "Application"
    create_standard_structure(path, package, app_class, run_app=True, is_module=True, with_profile=True)
    write_file(f"{path}/pom.xml", generate_module_pom(module))

# Create orchestrator with scanBasePackages, no profile on main class
orchestrator_path = f"{project_name}/{orchestrator}"
orchestrator_package = f"{group_id}.{orchestrator}"
app_class = "OrchestratorApplication"
scan_packages = [f"{group_id}.{m.replace('-', '')}" for m in modules] + [orchestrator_package]
create_standard_structure(orchestrator_path, orchestrator_package, app_class, run_app=True, scan_packages=scan_packages, with_profile=False)
write_file(f"{orchestrator_path}/pom.xml", generate_orchestrator_pom())

print(f"✅ Project '{project_name}' structure with profiles generated successfully.")




ping function
package com.example.orchestrator.utils;

import com.example.modulea.ModuleAPingPlugin;
import com.example.moduleb.ModuleBPingPlugin;
import com.example.modulec.ModuleCPingPlugin;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ModulePingRunner implements ApplicationRunner {

    private final ModuleAPingPlugin moduleAPingPlugin;
    private final ModuleBPingPlugin moduleBPingPlugin;
    private final ModuleCPingPlugin moduleCPingPlugin;

    public ModulePingRunner(ModuleAPingPlugin moduleAPingPlugin,
                            ModuleBPingPlugin moduleBPingPlugin,
                            ModuleCPingPlugin moduleCPingPlugin) {
        this.moduleAPingPlugin = moduleAPingPlugin;
        this.moduleBPingPlugin = moduleBPingPlugin;
        this.moduleCPingPlugin = moduleCPingPlugin;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Pinging all modules before startup...");

        System.out.println("Module A says: " + moduleAPingPlugin.getInfo());
        System.out.println("Module B says: " + moduleBPingPlugin.getInfo());
        System.out.println("Module C says: " + moduleCPingPlugin.getInfo());

        System.out.println("All modules pinged successfully.");
    }
}

