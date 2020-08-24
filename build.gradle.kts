/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

plugins {
  java
  id("nebula.release") version "15.1.0"
  id("com.diffplug.spotless") version "5.1.2"
}

val releaseTask = tasks.named("release")
val postReleaseTask = tasks.named("release")

allprojects {
  repositories {
    jcenter()
    mavenCentral()
    mavenLocal()

    maven {
      setUrl("https://oss.jfrog.org/libs-snapshot")
    }
  }

  project.group = "software.amazon.awsobservability"

  plugins.apply("com.diffplug.spotless")

  plugins.withType(BasePlugin::class) {
    val assemble = tasks.named("assemble")
    val check = tasks.named("check")

    releaseTask.configure {
      dependsOn(assemble, check)
    }
  }

  spotless {
    kotlinGradle {
      ktfmt("0.18")
    }
  }

  plugins.withId("java") {
    java {
      sourceCompatibility = JavaVersion.VERSION_1_7
      targetCompatibility = JavaVersion.VERSION_1_7
    }

    dependencies {
      configurations.configureEach {
        add(name, enforcedPlatform(project(":dependencyManagement")))
      }
    }

    spotless {
      java {
        googleJavaFormat("1.8")
      }
    }
  }

  plugins.withId("maven-publish") {
    val publishTask = tasks.named("publish")

    postReleaseTask.configure {
      dependsOn(publishTask)
    }

    configure<PublishingExtension> {
      publications {
        register<MavenPublication>("maven") {
          afterEvaluate {
            artifactId = project.findProperty("archivesBaseName") as String
          }

          plugins.withId("java-platform") {
            from(components["javaPlatform"])
          }
          plugins.withId("java-library") {
            from(components["java"])
          }
          plugins.withId("java") {
            from(components["java"])
          }

          versionMapping {
            allVariants {
              fromResolutionResult()
            }
          }

          pom {
            description.set(
                "The Amazon Web Services distribution of the OpenTelemetry Java Instrumentation.")

            licenses {
              license {
                name.set("Apache License, Version 2.0")
                url.set("https://aws.amazon.com/apache2.0")
                distribution.set("repo")
              }
            }

            developers {
              developer {
                id.set("amazonwebservices")
                organization.set("Amazon Web Services")
                organizationUrl.set("https://aws.amazon.com")
                roles.add("developer")
              }
            }
          }
        }
      }

      repositories {
        // For now, we only publish to GitHub Packages
        maven {
          name = "GitHubPackages"
          url = uri("https://maven.pkg.github.com/anuraaga/aws-opentelemetry-java-instrumentation")
          credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("PUBLISH_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("PUBLISH_PASSWORD")
          }
        }
      }
    }
  }
}
