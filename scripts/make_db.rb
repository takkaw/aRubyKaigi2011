#!/usr/bin/env ruby
# coding : utf-8

path = File.expand_path(File.dirname(__FILE__))

require 'sequel'
db_en = Sequel.sqlite(path + '/../assets/RubyKaigi2011en.db')
db_ja = Sequel.sqlite(path + '/../assets/RubyKaigi2011ja.db')

def create_table(db)
  unless db.table_exists? :RubyKaigi2011
    db.create_table :RubyKaigi2011 do
      primary_key :_id
      string :day
      string :room
      string :start
      string :end
      string :speaker
      string :title
      string :desc
      string :lang
    end 
  end
  unless db.table_exists? :android_metadata
    db.create_table :android_metadata do
      text :locale
    end
    db[:android_metadata] << {:locale => 'en_US'}
  end
end
create_table(db_en)
create_table(db_ja)

require 'yaml'
require 'date'

yamls = Dir.glob(path + '/rubykaigi/db/2011/room_timetables/*.yaml').sort!

rooms_en = {'M' => 'Main Hall','S' => 'Sub Hall'}
rooms_ja = {'M' => '大ホール','S' => '小ホール'}

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
        if presenters = ev['presenters']
          presenters.each { |pre|
            speakers_en << ' / ' unless speakers_en == ''
            speakers_en << pre['name']['en'] 
            speakers_ja << ' / ' unless speakers_ja == ''
            speakers_ja << (pre['name']['ja'] || pre['name']['en']) 
          }
        end
        if abstract = ev['abstract']
          abstract_en = abstract['en']
          abstract_ja = abstract['ja'] || abstract_en
          abstract_en = abstract_ja if abstract_en == 'TBD'
        end

        lang = ( ev['language'] || '' ).gsub('English','en').gsub('Japanese','ja')

        db_en[:RubyKaigi2011] << {
          :day => day,
          :room => room_en,
          :start => start,
          :end => _end,
          :title => title_en,
          :speaker => speakers_en,
          :desc => abstract_en,
          :lang => lang
        }
        db_ja[:RubyKaigi2011] << {
          :day => day,
          :room => room_ja,
          :start => start,
          :end => _end,
          :title => title_ja,
          :speaker => speakers_ja,
          :desc => abstract_ja,
          :lang => lang
        }
      } if event
  }
}

