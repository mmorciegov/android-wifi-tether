#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

int main(int argc, char *argv[]) {
	if (argc != 2) {
		fprintf(stderr, "Usage: tetherexec <tether-command>.\n");
		return -1;
	}
	if (setuid(0)) {
		fprintf(stderr, "Need root permission.\n");
		return -1;
	}
	char command[500] = "/data/data/android.tether/bin/tether ";
	strcat(command, argv[1]);
	return system(command);
}

