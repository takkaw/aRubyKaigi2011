#!/usr/bin/env ruby
# coding : utf-8

path = File.expand_path(File.dirname(__FILE__))
db_path = path + '/../assets/RubyKaigi2011.db'

begin
File.delete(db_path)
rescue
end

require 'sequel'
db = Sequel.sqlite(db_path)

unless db.table_exists? :RubyKaigi2011
  db.create_table :RubyKaigi2011 do
    primary_key :_id
    string :day
    string :room_en
    string :room_ja
    string :start
    string :end
    string :speaker_en
    string :speaker_ja
    string :title_en
    string :title_ja
    string :desc_en
    string :desc_ja
    string :lang
    string :speaker_bio_en
    string :speaker_bio_ja
  end 
end
unless db.table_exists? :android_metadata
  db.create_table :android_metadata do
    text :locale
  end
  db[:android_metadata] << {:locale => 'en_US'}
end

require 'yaml'
require 'date'

yamls = Dir.glob(path + '/rubykaigi/db/2011/room_timetables/*.yaml').sort!

rooms_en = {'M' => 'Main Hall','S' => 'Sub Hall'}
rooms_ja = {'M' => '大ホール','S' => '小ホール'}

special_event = ['Open','Break','Lunch','Transit time','Party at Ikebukuro']

yamls.each { |yaml|
  y = YAML.load_file(yaml)

  day = y['date'].day
  room_en = rooms_en[y['room_id']]
  room_ja = rooms_ja[y['room_id']] || room_en

  y['timeslots'].each { |slots|
    event = slots['event_ids']
    event.each { |ev|
      day = slots['start'].day
      start = "%02d:%02d" % [slots['start'].hour, slots['start'].min]
      _end = "%02d:%02d" % [slots['end'].hour,slots['end'].min]
      ev.to_s

      ev_yaml = path + '/rubykaigi/db/2011/events/%s.yaml' % ev 
      ev = YAML.load_file(ev_yaml)

      title_en = ev['title']['en']
      title_ja = ev['title']['ja'] || ev['title']['en']

      speakers_en = ''
      speakers_ja = ''
      speakers_bio_en = ''
      speakers_bio_ja = ''
      if presenters = ev['presenters']
        presenters.each { |pre|
          speakers_en << ' / ' unless speakers_en == ''
          speakers_en << pre['name']['en'] 
          speakers_ja << ' / ' unless speakers_ja == ''
          speakers_ja << (pre['name']['ja'] || pre['name']['en']) 

          bio_en = pre['name']['en']
          bio_en << "\n(" + pre['affiliation']['en'] + ")" if pre['affiliation']['en']
          bio_en << "\n" + pre['bio']['en'] if pre['bio']['en']
          speakers_bio_en << "\n\n" unless speakers_bio_en == ''
          speakers_bio_en << bio_en

          bio_ja = pre['name']['ja'] || pre['name']['en']
          bio_ja << "\n(" + (pre['affiliation']['ja'] || pre['affiliation']['en']) + ")" if (pre['affiliation']['ja'] || pre['affiliation']['en'])
          bio_ja << "\n" + (pre['bio']['ja'] || pre['bio']['en']) if (pre['bio']['ja'] || pre['bio']['en'])
          speakers_bio_ja << "\n\n" unless speakers_bio_ja == ''
          speakers_bio_ja << bio_ja
        }
      end
      if abstract = ev['abstract']
        abstract_en = abstract['en']
        abstract_ja = abstract['ja'] || abstract_en
        abstract_en = abstract_ja if abstract_en == 'TBD'
      end

      lang = ( ev['language'] || '' ).gsub('English','en').gsub('Japanese','ja')
      lang = "[%s]" % lang unless lang == ''

      special = special_event.include? title_en

      break if special && room_en == 'Sub Hall'

      db[:RubyKaigi2011] << {
        :day => day,
        :room_en => special ? '' : room_en,
        :room_ja => special ? '' : room_ja,
        :start => start,
        :end => _end,
        :title_en => title_en,
        :title_ja => title_ja,
        :speaker_en => speakers_en,
        :speaker_ja => speakers_ja,
        :desc_en => abstract_en,
        :desc_ja => abstract_ja,
        :lang => lang,
        :speaker_bio_en => speakers_bio_en,
        :speaker_bio_ja => speakers_bio_ja
      }
    } if event
  }
}

