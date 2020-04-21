require 'java'
require 'builder'
require 'rubygems/indexer.rb'
# @todo #32:120min Gem submission implementation.
#  The implementation must receive the .gem file, unzip it, and update specs files. As a result,
#  files become available for downloading.
class SubmitGem
  java_import com.artipie.http.async.AsyncResponse
  java_import com.artipie.http.rs.RsWithStatus
  java_import com.artipie.http.rs.RsStatus
  java_import com.artipie.asto.fs.RxFile
  java_import Java::hu.akarnokd.rxjava2.interop.SingleInterop
  java_import Java::io.reactivex.Single
  java_import java.nio.file.Paths
  java_import org.slf4j.LoggerFactory
  include com.artipie.http.Slice

  @@log = LoggerFactory::getLogger("com.artipie.gem.SubmitGem")

  def initialize(storage, vertx)
    @storage = storage
    @vertx = vertx
    @idx = "temp-gem-index"
    @gems = File.join(@idx, "gems")
    puts @gems.class
    Dir.mkdir(@idx) unless File.exists?(@idx)
    Dir.mkdir(@gems) unless File.exists?(@gems)
    @indexer = Gem::Indexer.new(@idx)
  end

  def response(line, headers, body)
    @@log.debug("Requested #{line}")
    @indexer.generate_index
    AsyncResponse.new(
        # @todo #9:30min Random gem name generation.
        #  Currently, when gem is uploaded, it has a name 'upd.gem'. The approach does not
        #  allow us to upload concurrently. Names should chosen randomly.
        RxFile.new(Paths::get(@gems, "upd.gem"), @vertx.fileSystem()).save(body).and_then(
            Single::from_callable {
              # @todo #9:30min Sync generated indexes with Storage.
              #  For now, generated indexes are stored locally in temp-gem-index directory.
              #  Those should also be syncronized with storage.
              @indexer.generate_index
              RsWithStatus.new(RsStatus::OK)
            }
        ).to(SingleInterop::get())
    )
  end
end