# frozen_string_literal: true
require 'rubygems/indexer.rb'

class MetaRunner

    def initialize(val)
        dir1 = File.dirname(val)
        dir2 = File.expand_path("..", dir1)
        spec = Gem::Package.new(val).spec()
        metas = []
        metafiles = ['latest_specs.4.8', 'specs.4.8']
        metadata = []
        metafiles.each do |f|
            metas = []
            found = false
            fullpath = dir2 + '/' + f
            content = ''
            if File.file?(fullpath)
                content = File.open(fullpath).read
                metas = Marshal.load(content)
                metas.each do |item|
                    if item[0] == spec.name && item[1].version == spec.version.version
                        found = true
                    end
                end
            end
            if found == false
                metas.push([spec.name, Gem::Version.create(spec.version.version), "ruby"])
                data = Marshal.dump(metas)
                metadata.push(data)
            else
                metadata.push(content)
            end
        end

        Gem::Indexer.new(dir2, {build_modern:true}).generate_index
        ind = 0
        metadata.each do |data|
            fullpath = dir2 + '/' + metafiles[ind]
            File.write(fullpath, data)
            Zlib::GzipWriter.open(fullpath + '.gz') do |gz|
                gz.write data
            ind = ind + 1
        end
    end
end
