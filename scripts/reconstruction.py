#!/usr/bin/env python
import re
import os
import genViewId

source="../src/com/tigerknows/Sphinx.java"
replace_dir="../src/com/tigerknows"
trans={}
id_dict={}

def walk_dir(dir,topdown=True):
	for root, dirs, files in os.walk(dir, topdown):
		for name in files:
			j_file=open(os.path.join(root,name), 'r+');
			content=j_file.read();
			for k,v in trans.iteritems():
				content=re.sub("(mSphinx\.|sphinx\.)?(" + v + ")", 
						"mFragmentManager.\g<2>", content)
				#content=re.sub(v + "\(\)\.", "((%s)getFragment(ViewIDs.%s))."%(k,id_dict[k]), content)
				#content=re.sub(v + "\(\)", "getFragment(TKFragmentManager.%s)"%id_dict[k], content)
			j_file.seek(0)
			j_file.write(content)

if __name__ == "__main__":
	s_file=open(source, 'r');
	s_lines=s_file.readlines();
	regx = re.compile("public (\w*ent) (get.+Fragment)")
	#trans = {m.group(1):m.group(2) for s_line in s_lines if 
			#(m = regx.search(s_line))}
	for s_line in s_lines:
		m = re.search("public (\w*ent) (get.+Fragment)", s_line)
		if m:
			trans[m.group(1)] = m.group(2)

	trans.pop("TitleFragment")
	trans.pop("HomeBottomFragment")
	trans.pop("BaseFragment")
	#print [(v.split('.')[-1],k) for k,v in genViewId.IDs.items()]
	id_dict = {v.split('.')[-1]:k for k,v in genViewId.IDs.items()}
	print trans
	print id_dict
	walk_dir(replace_dir)
