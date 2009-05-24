#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
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

JNIEXPORT jint JNICALL Java_android_tether_system_NativeTask_runCommand
  (JNIEnv *env, jclass class, jstring parameter)
{
  const char *parameterString;
  parameterString = (*env)->GetStringUTFChars(env, parameter, 0);
  
  char command[500] = "/system/bin/tetherexec ";
  strcat(command, parameterString);
  
  (*env)->ReleaseStringUTFChars(env, parameter, parameterString);  
  return (jint)system(command);
}
