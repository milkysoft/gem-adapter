# frozen_string_literal: true
require 'rubygems'
require 'rubygems/package'
require 'time'
require 'tmpdir'
require 'digest'
require 'json'
require 'java'

class Ex
    include com.artipie.gem.IRubyInfo

    def initialize(val)
        @val = val
    end

    def info()
        spec = Gem::Package.new(@val).spec
        metas = Marshal.load(File.open("latest_specs.4.8").read)
        found = false
        metas.each do |item|
            if item[0] == spec.name && item[1].version == spec.version.version
                found = true
                puts 'Found specification'
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
        end
    end
end
