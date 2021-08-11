# frozen_string_literal: true
require 'rubygems'
require 'rubygems/package'
require 'time'
require 'tmpdir'
require 'java'

class Ex
    def initialize(val)
        @val = val
    end

    def info()
        puts "222222"
        puts @val
        begin
            spec = Gem::Package.new(@val).spec()
            found = false
            metas = []
            if File.file?("latest_specs.4.8") and File.size("latest_specs.4.8") > 10
                puts "Check latest"
                content = File.open("latest_specs.4.8").read
                puts content
                metas = Marshal.load(content)
                metas.each do |item|
                    if item[0] == spec.name && item[1].version == spec.version.version
                        found = true
                        puts 'Found specification'
                    end
                end
            end
            if found == false
                puts 'Did not found specification'
                metas.push([spec.name, Gem::Version.create(spec.version.version), "ruby"])
                data = Marshal.dump(metas)
                File.write('latest_specs.4.8', data)
                Zlib::GzipWriter.open('latest_specs.4.8.gz') do |gz|
                    gz.write data
                end
                puts "Final spec"
       	        puts data
            end
        rescue Exception => e
            puts e
        end
    end
end
