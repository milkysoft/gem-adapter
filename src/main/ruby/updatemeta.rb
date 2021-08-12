# frozen_string_literal: true
require 'rubygems'
require 'rubygems/package'
require 'time'
require 'java'

class Ex
    def initialize(val)
        @val = val
    end

    def info()
        begin
            spec = Gem::Package.new(@val).spec()
            found = false
            metas = []
            f = 'latest_specs.4.8'
            if File.file?(f) and File.size(f) > 10
                content = File.open(f).read
                metas = Marshal.load(content)
                metas.each do |item|
                    if item[0] == spec.name && item[1].version == spec.version.version
                        found = true
                        puts 'Found specification in latest'
                    end
                end
            end
            if found == false
                puts 'Did not found specification in latest'
                metas.push([spec.name, Gem::Version.create(spec.version.version), "ruby"])
                data = Marshal.dump(metas)
                File.write(f, data)
                Zlib::GzipWriter.open(f + '.gz') do |gz|
                    gz.write data
                end
            end

            found = false
            metas = []
       	    f =	'specs.4.8'
            if File.file?(f) and File.size(f) > 10
                content = File.open(f).read
                metas = Marshal.load(content)
                metas.each do |item|
                    if item[0] == spec.name && item[1].version == spec.version.version
                        found = true
                        puts 'Found specification in specs'
                    end
                end
            end
            if found == false
                puts 'Did not found specification in specs'
                metas.push([spec.name, Gem::Version.create(spec.version.version), "ruby"])
                data = Marshal.dump(metas)
                File.write(f, data)
                Zlib::GzipWriter.open(f + '.gz') do |gz|
                    gz.write data
                end
            end
        rescue Exception => e
            puts e
        end
    end
end
