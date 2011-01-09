# You'll need droid-wrapper for this to work.
# https://github.com/tmurakam/droid-wrapper

ST_SOURCES=st-backend.c
ST_EXECUTABLE=../assets/st-backend

CC=droid-gcc
CFLAGS=-I$(DROID_ROOT/external/openssl/include -fomit-frame-pointer -O3 -funroll-all-loops -l crypto -Wall
STRIP=droid-strip

all: $(ST_EXECUTABLE)

$(ST_EXECUTABLE): $(ST_SOURCES)
	$(CC) $(CFLAGS) -o $@ $(ST_SOURCES)
	$(STRIP) $@

clean:
	rm -f $(ST_EXECUTABLE)