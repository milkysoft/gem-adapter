require 'java'
# @todo #32:120min Gem submission implementation.
#  The implementation must receive the .gem file, unzip it, and update specs files. As a result,
#  files become available for downloading.
class SubmitGem
  include com.artipie.http.Slice
  def response(line, headers, body)
    com.artipie.http.rs.RsWithStatus.new(com.artipie.http.rs.RsStatus::NOT_IMPLEMENTED)
  end
end