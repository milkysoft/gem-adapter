require 'java'
require 'builder'
require 'rubygems/indexer.rb'
require 'securerandom'
require 'AstoIndexer.rb'
# @todo #32:120min Gem submission implementation.
#  The implementation must receive the .gem file, unzip it, and update specs files. As a result,
#  files become available for downloading.
class SubmitGem
  java_import com.artipie.http.async.AsyncResponse
  java_import com.artipie.http.rs.RsWithStatus
  java_import com.artipie.http.rs.RsStatus
  java_import com.artipie.asto.rx.RxCopy
  java_import com.artipie.asto.rx.RxStorageWrapper
  java_import com.artipie.asto.fs.FileStorage
  java_import com.artipie.asto.fs.RxFile
  java_import com.artipie.asto.Key
  java_import Java::io.reactivex.Flowable
  java_import Java::io.reactivex.Single
  java_import Java::io.reactivex.Completable
  java_import java.util.concurrent.TimeUnit
  java_import java.nio.file.Paths
  java_import java.util.ArrayList
  java_import java.util.HashSet
  java_import org.slf4j.LoggerFactory
  include com.artipie.http.Slice

  @@log = LoggerFactory::getLogger("com.artipie.gem.SubmitGem")

  def initialize(storage, fs)
    @storage = storage
    @fs = fs
    @idx = "temp-gem-index"
    @gems = File.join(@idx, "gems")
    # Was index created before?
    idx_existed = File.exists?(@idx)
    Dir.mkdir(@idx) unless idx_existed
    Dir.mkdir(@gems) unless File.exists?(@gems)
    @indexer = AstoIndexer.new(storage ,@idx, { build_modern: true })
    @indexer.generate_index unless idx_existed
    @rx_idx_local = RxStorageWrapper.new(FileStorage.new(Paths::get(@idx), @fs))
    @rx_storage = RxStorageWrapper.new(@storage)
  end

  def response(line, headers, body)
    @@log.debug("Requested #{line}")
    local = SecureRandom.hex(32) + ".gem"
    AsyncResponse.new(
        RxFile.new(Paths::get(@gems, local), @fs).save(body)
            .and_then(self.sync(@rx_storage, @rx_idx_local))
            .and_then(Completable::from_action { @indexer.update_index })
            .and_then(self.sync(@rx_idx_local, @rx_storage))
            .and_then(Single::just(RsWithStatus.new(RsStatus::OK)))
    )
  end

  #
  # from - RxStorage to sync from
  # to - RxStorage to sync with
  def sync(from, to)
    # Specs files to copy if exists
    specs = from.list(Key::ROOT).flatMapPublisher { |keys|
      keys.retainAll(
          HashSet.new(
              [
                  Key::From.new("latest_specs.4.8"),
                  Key::From.new("latest_specs.4.8.gz"),
                  Key::From.new("prerelease_specs.4.8"),
                  Key::From.new("prerelease_specs.4.8.gz"),
                  Key::From.new("specs.4.8"),
                  Key::From.new("specs.4.8.gz")
              ]
          )
      )
      Flowable::from_iterable(keys)
    }
    # Non-specs files, copy if not exists
    diff = from.list(Key::From.new("quick"))
               .zipWith(
                   to.list(Key::From.new("quick")),
                   -> src, dest {
                     src.removeAll(dest)
                     src
                   }
               )
               .flatMapPublisher { |keys| Flowable::from_iterable(keys) }
    specs.mergeWith(diff).to_list.flatMapCompletable do |keys|
      @@log.debug("Copy {}", keys)
      RxCopy.new(from, keys).copy(to)
    end
  end
end