# GNU makefile. https://www.gnu.org/software/make/manual/make.html

.PHONY: dactylmanuform_46key dmote_62key concertina_64key macropad_12key vis low mutual caseside all docs test clean
.DEFAULT_GOAL := dactylmanuform_46key

COMMA := ,
NEW_ONLY := --unchanged-line-format='' --new-line-format='%L'

# Real prior artefacts are charted as this makefile is parsed.
SOURCECODE := $(shell find src -type f)

# CONFFILES is a space-separated array of relative paths to selected
# YAML files, starting with a near-neutral base.
CONFFILES := config/base.yaml

# TO_SCAD evaluates to a Java CLI call.
TO_SCAD = java -jar target/dmote.jar $(foreach FILE,$(CONFFILES),-c $(FILE))

# The render_2d function creates an image-file target, typically a PNG.
# Argument 1: Base name of source application output.
#          2: Viewport position.
#          3: Viewport angle.
#          4: Viewer distance.
#          5: Picture resolution.
define render_2d
	$(eval CONFFILES := $(filter %.yaml,$^))
	$(TO_SCAD)
	openscad -o $@ --camera $(subst :,$(COMMA),$2),$(subst :,$(COMMA),$3),$4 --imgsize $(subst :,$(COMMA),$5) --render 1 things/scad/$1.scad
endef

# The remainder of this file describes more typical Make work, starting with
# the compilation of the Clojure application into a Java .jar, specific
# pieces of documentation, and illustrations for documentation.

target/dmote.jar: $(SOURCECODE)
	lein uberjar

doc/img/butty/bare.png: target/dmote.jar
	$(call render_2d,body-main,0:0:0,0:0:0,0,300:200)

doc/img/butty/min.png: target/dmote.jar resources/butty/config/02.yaml
	$(call render_2d,body-main,0:0:8,50:0:50,70,400:260)

doc/img/butty/base.png: target/dmote.jar config/base.yaml resources/butty/config/02.yaml
	$(call render_2d,body-main,0:0:8,50:0:50,70,400:260)

doc/img/butty/bevel.png: target/dmote.jar config/base.yaml resources/butty/config/12.yaml
	$(call render_2d,body-main,0:0:6,50:0:50,70,400:260)

doc/img/butty/to-ground.png: target/dmote.jar config/base.yaml resources/butty/config/14.yaml
	$(call render_2d,body-main,0:0:3,50:0:50,80,400:330)

doc/img/butty/open-back-front.png: target/dmote.jar config/base.yaml resources/butty/config/16.yaml
	$(call render_2d,body-main,0:0:3,50:0:50,80,400:330)

doc/img/butty/open-back-rear.png: target/dmote.jar config/base.yaml resources/butty/config/16.yaml
	$(call render_2d,body-main,0:0:6,120:0:150,78,400:300)

doc/img/butty/rear-housing.png: target/dmote.jar config/base.yaml resources/butty/config/22.yaml
	$(call render_2d,body-main,12:0:17,50:0:40,100,500:400)

doc/img/butty/mcu-1-default.png: target/dmote.jar config/base.yaml resources/butty/config/24.yaml
	$(call render_2d,body-main,8:0:13,55:0:35,90,500:400)

doc/img/butty/mcu-2-preview.png: target/dmote.jar config/base.yaml resources/butty/config/26.yaml
	$(call render_2d,body-main,0:0:0,60:0:30,130,500:420)

doc/img/butty/mcu-3-inplace.png: target/dmote.jar config/base.yaml resources/butty/config/28a.yaml
	$(call render_2d,body-main,10:0:-11,135:0:35,100,500:420)

doc/img/butty/mcu-4-port.png: target/dmote.jar config/base.yaml resources/butty/config/28b.yaml
	$(call render_2d,body-main,-9:0:-8,70:0:165,110,500:440)

doc/img/butty/tweak-roof-1.png: target/dmote.jar config/base.yaml resources/butty/config/32.yaml
	$(call render_2d,body-main,0:14:3,70:0:90,45,500:500)

doc/img/butty/tweak-roof-2.png: target/dmote.jar config/base.yaml resources/butty/config/34a.yaml
	$(call render_2d,body-main,0:14:6,70:0:90,30,500:280)

doc/img/butty/tweak-roof-3.png: target/dmote.jar config/base.yaml resources/butty/config/34b.yaml
	$(call render_2d,body-main,0:14:6,70:0:90,30,500:280)

doc/img/butty/tweak-roof-4.png: target/dmote.jar config/base.yaml resources/butty/config/34c.yaml
	$(call render_2d,body-main,0:14:6,70:0:90,30,500:280)

doc/img/butty/tweak-roof-5-side.png: target/dmote.jar config/base.yaml resources/butty/config/34d.yaml
	$(call render_2d,body-main,0:14:6,70:0:90,30,500:280)

doc/img/butty/tweak-roof-5-bottom.png: target/dmote.jar config/base.yaml resources/butty/config/34d.yaml
	$(call render_2d,body-main,0:14:6.5,100:0:90,36,500:400)

doc/img/butty/tweak-roof-6-bottom.png: target/dmote.jar config/base.yaml resources/butty/config/34e.yaml
	$(call render_2d,body-main,0:14:6.5,100:0:90,36,500:400)

doc/img/butty/tweak-wall-side.png: target/dmote.jar config/base.yaml resources/butty/config/36.yaml
	$(call render_2d,body-main,8:0:13,55:0:35,90,500:400)

doc/img/butty/tweak-wall-bottom.png: target/dmote.jar config/base.yaml resources/butty/config/36.yaml
	$(call render_2d,body-main,10:0:-11,135:0:35,100,500:420)

doc/img/butty/bottom-1-base.png: target/dmote.jar config/base.yaml resources/butty/config/42.yaml
	$(call render_2d,bottom-plate-case,14:0:17,45:0:45,85,500:380)

doc/img/butty/bottom-2-tweak.png: target/dmote.jar config/base.yaml resources/butty/config/44.yaml
	$(call render_2d,bottom-plate-case,14:0:17,45:0:45,85,500:380)

doc/img/butty/bottom-3-fasteners.png: target/dmote.jar config/base.yaml resources/butty/config/46.yaml
	$(call render_2d,bottom-plate-case,14:0:17,45:0:45,85,500:380)

doc/img/butty/main-body-fasteners.png: target/dmote.jar config/base.yaml resources/butty/config/46.yaml
	$(call render_2d,body-main,-13:0:32,135:0:150,160,500:500)

doc/options-main.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters main > $@

doc/options-central.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters central > $@

doc/options-clusters.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters clusters > $@

doc/options-nested.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters nested > $@

doc/options-flanges.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters flanges > $@

doc/options-ports.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters ports > $@

doc/options-wrist-rest-mounts.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters wrist-rest-mounts > $@

doc/tutorial-1a.md: resources/butty/*/*
	cat resources/butty/doc/artefact_alert.md \
			resources/butty/doc/01.md \
			resources/yamlblock_begin.md resources/butty/config/02.yaml resources/yamlblock_end.md \
			resources/butty/doc/03.md \
			resources/yamlblock_begin.md resources/butty/config/04.yaml resources/yamlblock_end.md \
			resources/butty/doc/05.md \
			> $@

doc/tutorial-1b.md: resources/butty/*/*
	cat resources/butty/doc/artefact_alert.md \
			resources/butty/doc/11.md \
			resources/yamlblock_begin.md resources/butty/config/12.yaml resources/yamlblock_end.md \
			resources/butty/doc/13.md \
			resources/yamlblock_begin.md resources/butty/config/14.yaml resources/yamlblock_end.md \
			resources/butty/doc/15.md \
			resources/yamlblock_begin.md resources/butty/config/16.yaml resources/yamlblock_end.md \
			resources/butty/doc/17.md \
			> $@

doc/tutorial-1c.md: resources/butty/*/*
	! diff resources/butty/config/22.yaml resources/butty/config/24.yaml $(NEW_ONLY) > /tmp/24.yaml
	! diff resources/butty/config/22.yaml resources/butty/config/26.yaml $(NEW_ONLY) > /tmp/26.yaml
	! diff resources/butty/config/22.yaml resources/butty/config/28a.yaml $(NEW_ONLY) > /tmp/28.yaml
	cat resources/butty/doc/artefact_alert.md \
			resources/butty/doc/21.md \
			resources/yamlblock_begin.md resources/butty/config/22.yaml resources/yamlblock_end.md \
			resources/butty/doc/23.md \
			resources/yamlblock_begin.md /tmp/24.yaml resources/yamlblock_end.md \
			resources/butty/doc/25.md \
			resources/yamlblock_begin.md /tmp/26.yaml resources/yamlblock_end.md \
			resources/butty/doc/27.md \
			resources/yamlblock_begin.md /tmp/28.yaml resources/yamlblock_end.md \
			resources/butty/doc/29.md \
			> $@

doc/tutorial-1d.md: resources/butty/*/*
	! diff resources/butty/config/28b.yaml resources/butty/config/32.yaml $(NEW_ONLY) > /tmp/32.yaml
	! diff resources/butty/config/32.yaml resources/butty/config/34a.yaml $(NEW_ONLY) > /tmp/34a.yaml
	! diff resources/butty/config/32.yaml resources/butty/config/34b.yaml $(NEW_ONLY) > /tmp/34b.yaml
	! diff resources/butty/config/32.yaml resources/butty/config/34c.yaml $(NEW_ONLY) > /tmp/34c.yaml
	! diff resources/butty/config/32.yaml resources/butty/config/34d.yaml $(NEW_ONLY) > /tmp/34d.yaml
	! diff resources/butty/config/32.yaml resources/butty/config/34e.yaml $(NEW_ONLY) > /tmp/34e.yaml
	! diff resources/butty/config/34e.yaml resources/butty/config/36.yaml $(NEW_ONLY) > /tmp/36.yaml
	cat resources/butty/doc/artefact_alert.md \
			resources/butty/doc/31.md \
			resources/yamlblock_begin.md resources/butty/config/32.yaml resources/yamlblock_end.md \
			resources/butty/doc/33a.md \
			resources/yamlblock_begin.md /tmp/32.yaml resources/yamlblock_end.md \
			resources/butty/doc/33b.md \
			resources/yamlblock_begin.md /tmp/34a.yaml resources/yamlblock_end.md \
			resources/butty/doc/35a.md \
			resources/yamlblock_begin.md /tmp/34b.yaml resources/yamlblock_end.md \
			resources/butty/doc/35b.md \
			resources/yamlblock_begin.md /tmp/34c.yaml resources/yamlblock_end.md \
			resources/butty/doc/35c.md \
			resources/yamlblock_begin.md /tmp/34d.yaml resources/yamlblock_end.md \
			resources/butty/doc/35d.md \
			resources/yamlblock_begin.md /tmp/34e.yaml resources/yamlblock_end.md \
			resources/butty/doc/35e.md \
			resources/yamlblock_begin.md /tmp/36.yaml resources/yamlblock_end.md \
			resources/butty/doc/37.md \
			> $@

doc/tutorial-1e.md: resources/butty/*/*
	! diff resources/butty/config/36.yaml resources/butty/config/42.yaml $(NEW_ONLY) > /tmp/42.yaml
	! diff resources/butty/config/42.yaml resources/butty/config/44.yaml $(NEW_ONLY) > /tmp/44.yaml
	! diff resources/butty/config/44.yaml resources/butty/config/46.yaml $(NEW_ONLY) > /tmp/46.yaml
	cat resources/butty/doc/artefact_alert.md \
			resources/butty/doc/41.md \
			resources/yamlblock_begin.md resources/butty/config/44.yaml resources/yamlblock_end.md \
			resources/butty/doc/43a.md \
			resources/yamlblock_begin.md /tmp/42.yaml resources/yamlblock_end.md \
			resources/butty/doc/43b.md \
			resources/yamlblock_begin.md /tmp/44.yaml resources/yamlblock_end.md \
			resources/butty/doc/45a.md \
			resources/yamlblock_begin.md resources/butty/config/46.yaml resources/yamlblock_end.md \
			resources/butty/doc/45b.md \
			resources/yamlblock_begin.md /tmp/46.yaml resources/yamlblock_end.md \
			resources/butty/doc/47.md \
			> $@

# Phony targets follow.

# The following are only useful with targets that hit TO_SCAD.
# They represent curated shorthand for configuration fragments.
vis: ; $(eval CONFFILES += config/visualization.yaml)
low: ; $(eval CONFFILES += config/low_resolution.yaml)
mutual: ; $(eval CONFFILES += config/dmote/wrist/threaded_mutual.yaml)
caseside: ; $(eval CONFFILES += config/dmote/wrist/threaded_caseside.yaml)

# The following are bundled designs, defining convenient ways to run the application.
dactylmanuform_46key: target/dmote.jar
	$(eval CONFFILES += config/dactyl_manuform/base.yaml)
	$(TO_SCAD)

dmote_62key: target/dmote.jar
	$(eval CONFFILES += config/dmote/base.yaml)
	$(TO_SCAD)

concertina_64key: target/dmote.jar
	$(eval CONFFILES += config/concertina/base.yaml)
	$(eval CONFFILES += config/concertina/assortment/base.yaml)
	$(eval CONFFILES += config/concertina/assortment/reset.yaml)
	$(eval CONFFILES += config/concertina/assortment/magnets/slits.yaml)
	$(eval CONFFILES += config/concertina/assortment/magnets/cylinder5x2p5_centre.yaml)
	$(TO_SCAD)

concertina_66key: target/dmote.jar
	$(eval CONFFILES += config/concertina/base.yaml)
	$(eval CONFFILES += config/concertina/assortment/base.yaml)
	$(eval CONFFILES += config/concertina/assortment/reset.yaml)
	$(eval CONFFILES += config/concertina/assortment/magnets/slits.yaml)
	$(eval CONFFILES += config/concertina/assortment/magnets/cylinder5x2p5_centre.yaml)
	$(eval CONFFILES += config/concertina/encoder/base.yaml)
	$(TO_SCAD)

macropad_12key: target/dmote.jar
	$(eval CONFFILES += config/macropad/base.yaml)
	$(TO_SCAD)

# Higher-level phony targets follow.

docs: doc/img/*/* doc/options-central.md doc/options-clusters.md doc/options-main.md doc/options-nested.md doc/options-flanges.md doc/options-ports.md doc/options-wrist-rest-mounts.md doc/tutorial-1a.md doc/tutorial-1b.md doc/tutorial-1c.md doc/tutorial-1d.md doc/tutorial-1e.md

test:
	lein test

# The “all” target is intended for code sanity checking before pushing a commit.
all: test docs
	lein run -c test/config/central_housing_1.yaml
	lein run -c test/config/mount_types.yaml
	make vis mutual dmote_62key

clean:
	-rm things/scad/*.scad
	-rmdir things/scad/
	-rm things/stl/*.stl
	-rmdir things/stl/
	lein clean
