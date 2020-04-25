var assert = require('assert');
var src = require(__dirname + '/../index');
var async = require('async')
var _ = require('underscore')
var cache = src({expiry:2});
describe('Simple Redis Cache', function(){
  it('should connect to a redis instance', function(done){
    //Give redis some time to init
    setTimeout(function(){
      assert.equal('localhost', cache.client.host);
      done();
    }, 100);
  });
  it('should set a key with a value', function(done){
    cache.set('test', 'this is a string', done);
  });
  it('should retrieve the value', function(done){
    cache.get('test', function(err, value){
      assert.equal('this is a string', value);
      done();
    });
  });
  it('the key should be valid still', function(done){
    setTimeout(function(){
      cache.get('test', function(err, value){
        assert.notEqual(null, value);
        done();
      })}, 1000);
  });
  it('the key should be invalid now', function(done){
    setTimeout(function(){
      cache.get('test', function(err, value){
        assert.equal(null, value);
        done();
      })}, 3000);
  });
  it('should be able to delete a key', function(done){
    cache.set('test2', 'whatever', function(err){
      cache.del('test2', function(){
        cache.get('test2', function(err, value){
          assert.equal(value, null);
          done()
        });
      });
    });
  });
  it('should have a function to get the cache id', function() {
    assert.ok(cache.id())
  })
  it('should allow setting cache id', function() {
    assert.equal(src({id:'id-test'}).id(), 'id-test')
  })
  it('should list the cache keys', function(done) {
    var keys = _.map([
      1,2,3,4,5,6,7,8,9,10,11,12
    ], String)
    async.each(keys, function(k, cb) {
      cache.set(k, new Date().toJSON(), cb)
    }, function() {
      cache.keys(function(err, ks) {
        _.each(keys, function(k) {
          assert.ok(ks.indexOf(k) !== -1)
        })
        cache.keys(function(er, ks) {
          var raws = _.map(keys, function(p) { return cache.id() + ':' + p })
          _.each(raws, function(k) {
            assert.ok(ks.indexOf(k) !== -1)
          })
          done()
        }, { raw: true })
      });
    })
  })
});
