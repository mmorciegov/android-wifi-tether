#include <stdio.h>
#include <cutils/properties.h>
#include <sys/system_properties.h>

#include "android_tether_system_NativeTask.h"

JNIEXPORT jstring JNICALL Java_android_tether_system_NativeTask_getProp
  (JNIEnv *env, jclass class, jstring name)
{
  //Get the native string from javaString
  const char *nameString;
  nameString = (*env)->GetStringUTFChars(env, name, 0);

  char value[PROPERTY_VALUE_MAX];
  char *default_value;
  jstring jstrOutput;
  
  default_value = "undefined";
  property_get(nameString, value, default_value);

  jstrOutput = (*env)->NewStringUTF(env, value);

  (*env)->ReleaseStringUTFChars(env, name, nameString);  

  return jstrOutput;
}

JNIEXPORT void JNICALL Java_android_tether_system_NativeTask_runRootCommand
  (JNIEnv *env, jclass class, jstring command)
{
  //Get the native string from javaString
  const char *commandString;
  commandString = (*env)->GetStringUTFChars(env, command, 0);

  system(commandString);

  (*env)->ReleaseStringUTFChars(env, command, commandString);  
}