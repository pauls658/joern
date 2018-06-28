#include <stdio.h>
#include <stdlib.h>

char **alloc_hb(int max_id) {
	char **hb = (char**)malloc(sizeof(char*)*(max_id + 1));
	int i;
	for (i = 0; i <= max_id; i++) {
		hb[i] = (char*) malloc(sizeof(char)*(max_id + 1));
	}
	return hb;
}

void init_hb(char **hb) {
	FILE *f;
	f = fopen("edge.facts", "r");
	int start, end;
	while (fscanf(f, "%d\t%d\n", &start, &end) != EOF) {
		printf("loaded: %d, %d\n", start, end);
		hb[start][end] = 1;
	}
	fclose(f);
}

void hb_closure(char **hb, int max_id) {
	char changed = 1;
	int a1, a2, a3;
	int loops = 0;
	while (changed == 1) {
		changed = 0;
		for (a1 = 0; a1 <= max_id; a1++) {
			for (a2 = 0; a2 <= max_id; a2++) {
				for (a3 = 0; a3 <= max_id; a3++) {
					if (hb[a1][a3] == 0 && hb[a1][a2] == 1 && hb[a2][a3] == 1) {
						hb[a1][a3] = 1;
						changed = 1;
					}
				}
			}
		}
		printf("loop %d completed\n", loops);
		loops++;
	}
}

int main(int argc, char **argv) {
	int max_id = 10528;
	char **hb = alloc_hb(max_id);
	init_hb(hb);
	hb_closure(hb, max_id);
}
