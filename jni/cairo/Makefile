PROG = cairo.lib 
CC = gcc
CFLAGS = -std=c99 -c -g -W -pg -lm -lz
COMPILE = $(CC) $(CFLAGS) 
OUTPATH = ./
SOURCES = $(wildcard *.c)
OBJS = $(patsubst %.c,%.o,$(SOURCES))

$(PROG): $(OBJS)
	ar rcs $@ $^
		@echo "All ok! Output:$(OUTPATH)$(PROG)"
			@echo ""

%.o: %.c *.h
	$(COMPILE) -o $@ $<
%.o: %.cpp *.h
	$(COMPILE) -o $@ $<


cl clean:
	rm -f $(OBJS) $(OUTPATH)$(PROG)

